package org.wocommunity.plugins.intellij.eogen.core;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOEntity;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOModel;
import org.wocommunity.plugins.intellij.eomodel.core.model.EORelationship;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

public class EOGeneratorRunner {
	private static final Logger LOG = Logger.getInstance(EOGeneratorRunner.class);
	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

	public static class GenerationResult {
		private boolean success = true;
		private final List<String> generatedFiles = new ArrayList<>();
		private final List<String> messages = new ArrayList<>();

		public boolean isSuccess() {
			return success;
		}

		public void setSuccess(boolean success) {
			this.success = success;
		}

		public List<String> getGeneratedFiles() {
			return generatedFiles;
		}

		public List<String> getMessages() {
			return messages;
		}
	}

	public static GenerationResult generate(Project project, File eogenFile, EOGeneratorModel eogenModel, ProgressIndicator indicator) {
		return generate(project, eogenFile, eogenModel, indicator, null);
	}

	public static GenerationResult generate(Project project, File eogenFile, EOGeneratorModel eogenModel,
										   ProgressIndicator indicator, Consumer<String> logConsumer) {
		GenerationResult result = new GenerationResult();

		log(logConsumer, result, "=== Starting EOGenerator for: " + eogenFile.getName() + " ===");
		log(logConsumer, result, "File path: " + eogenFile.getAbsolutePath());

		File workingDir = eogenFile.getParentFile();
		if (workingDir == null && project != null && project.getBasePath() != null) {
			workingDir = new File(project.getBasePath());
		}
		log(logConsumer, result, "Working directory (.eogen dir): " + (workingDir != null ? workingDir.getAbsolutePath() : "unknown"));

		// Determine module/project content root
		File projectRoot = findProjectOrContentRoot(project, eogenFile);
		if (projectRoot != null) {
			log(logConsumer, result, "Project / Content root: " + projectRoot.getAbsolutePath());
		}

		// 1. Check for external executable
		String extPath = eogenModel.getEOGeneratorPath();
		if (extPath != null && !extPath.trim().isEmpty()) {
			File extExe = new File(extPath);
			if (extExe.exists() && extExe.canExecute()) {
				log(logConsumer, result, "Using external EOGenerator executable: " + extPath);
				return runExternal(project, workingDir, eogenModel, result, logConsumer);
			}
		}

		// 2. Built-in Generation
		try {
			File superclassDest = resolveDestination(project, workingDir, projectRoot, eogenModel.getDestination());
			File subclassDest = resolveDestination(project, workingDir, projectRoot, eogenModel.getSubclassDestination());

			log(logConsumer, result, "Resolved superclass destination: " + superclassDest.getAbsolutePath());
			log(logConsumer, result, "Resolved subclass destination:   " + subclassDest.getAbsolutePath());
			log(logConsumer, result, "Superclass package setting:      " + (eogenModel.getSuperclassPackage() != null ? eogenModel.getSuperclassPackage() : "(none)"));

			if (!superclassDest.exists() && !superclassDest.mkdirs()) {
				throw new IOException("Failed to create destination directory: " + superclassDest.getAbsolutePath());
			}
			if (!subclassDest.exists() && !subclassDest.mkdirs()) {
				throw new IOException("Failed to create subclass destination directory: " + subclassDest.getAbsolutePath());
			}

			// Initialize Velocity Engine
			VelocityEngine velocityEngine = new VelocityEngine();
			velocityEngine.setProperty("input.encoding", "UTF-8");
			velocityEngine.setProperty("output.encoding", "UTF-8");
			velocityEngine.setProperty("parser.space_gobbling", "bc");
			velocityEngine.init();

			boolean isWonder = isWonderProject(project, projectRoot, eogenModel);
			log(logConsumer, result, "Project type detected: " + (isWonder ? "WebObjects with Project Wonder / ERExtensions" : "Standard WebObjects"));

			ResolvedTemplate superclassTemplate = resolveTemplate(workingDir, projectRoot, eogenModel.getTemplateDir(),
					eogenModel.getJavaTemplate(), true, isWonder);
			ResolvedTemplate subclassTemplate = resolveTemplate(workingDir, projectRoot, eogenModel.getTemplateDir(),
					eogenModel.getSubclassJavaTemplate(), false, isWonder);

			log(logConsumer, result, "Selected superclass template: '" + superclassTemplate.name + "'");
			log(logConsumer, result, "  -> Loaded from: " + superclassTemplate.sourceDescription);
			log(logConsumer, result, "Selected subclass template:   '" + subclassTemplate.name + "'");
			log(logConsumer, result, "  -> Loaded from: " + subclassTemplate.sourceDescription);

			String superclassTemplateContent = superclassTemplate.content;
			String subclassTemplateContent = subclassTemplate.content;

			List<String> modelsToLoad = new ArrayList<>(eogenModel.getModels());
			if (modelsToLoad.isEmpty()) {
				log(logConsumer, result, "No -model arguments defined in .eogen file. Searching for matching .eomodeld...");
				String baseName = eogenFile.getName();
				int dot = baseName.lastIndexOf('.');
				if (dot > 0) {
					baseName = baseName.substring(0, dot);
				}
				File inferred = resolveModel(project, workingDir, projectRoot, baseName + ".eomodeld");
				if (inferred != null && inferred.exists()) {
					log(logConsumer, result, "Found matching model: " + inferred.getName() + " -> " + inferred.getAbsolutePath());
					modelsToLoad.add(inferred.getAbsolutePath());
				} else {
					log(logConsumer, result, "WARNING: No model could be inferred for " + eogenFile.getName());
				}
			}

			log(logConsumer, result, "Models to process: " + modelsToLoad.size());

			// Load each model into a shared EOModelGroup
			List<EOModel> loadedModels = new ArrayList<>();
			org.wocommunity.plugins.intellij.eomodel.core.model.EOModelGroup modelGroup =
					new org.wocommunity.plugins.intellij.eomodel.core.model.EOModelGroup();

			for (String modelPath : modelsToLoad) {
				log(logConsumer, result, "--- Resolving model: '" + modelPath + "' ---");
				File modelFolder = resolveModel(project, workingDir, projectRoot, modelPath);

				if (modelFolder == null || !modelFolder.exists()) {
					String msg = "ERROR: Model folder not found for '" + modelPath + "'!";
					log(logConsumer, result, msg);
					log(logConsumer, result, "Checked:\n  1. " + new File(workingDir, modelPath).getAbsolutePath() +
							(projectRoot != null ? "\n  2. " + new File(projectRoot, modelPath).getAbsolutePath() +
									"\n  3. " + new File(projectRoot, "Resources/" + modelPath).getAbsolutePath() +
									"\n  4. " + new File(projectRoot, "src/main/resources/" + modelPath).getAbsolutePath() : ""));
					continue;
				}

				log(logConsumer, result, "Found model folder: " + modelFolder.getAbsolutePath());
				EOModel eoModel = new EOModel();
				eoModel.loadFromFolder(modelFolder);
				loadedModels.add(eoModel);
				modelGroup.addModel(eoModel);

				List<EOEntity> entities = eoModel.getEntities();
				log(logConsumer, result, "Loaded model '" + eoModel.getName() + "' with " + entities.size() + " entities: " +
						entities.stream().map(EOEntity::getName).toList());

				if (entities.isEmpty()) {
					log(logConsumer, result, "WARNING: Model '" + eoModel.getName() + "' has 0 entities.");
				}
			}

			// Re-resolve all models in context of the complete model group
			for (EOModel eoModel : loadedModels) {
				eoModel.resolveModel();
			}

			// Generate entities
			for (EOModel eoModel : loadedModels) {
				for (EOEntity entity : eoModel.getEntities()) {
					if (indicator != null) {
						indicator.setText("Generating " + entity.getName() + "...");
					}

					generateEntity(velocityEngine, eoModel, entity, eogenModel, superclassDest, subclassDest,
							superclassTemplateContent, subclassTemplateContent, result, logConsumer);
				}
			}

			// Refresh VFS
			if (ApplicationManager.getApplication() != null) {
				VirtualFile superclassVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(superclassDest);
				if (superclassVf != null) {
					superclassVf.refresh(true, true);
				}
				VirtualFile subclassVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(subclassDest);
				if (subclassVf != null) {
					subclassVf.refresh(true, true);
				}
			}

			if (result.getGeneratedFiles().isEmpty()) {
				result.setSuccess(false);
				log(logConsumer, result, "=========================================================");
				log(logConsumer, result, "RESULT: 0 files were generated!");
				log(logConsumer, result, "Diagnostics:");
				log(logConsumer, result, " • If models were not found, check the '-model' paths in the .eogen Configuration Form.");
				log(logConsumer, result, " • If entities were skipped because of 'className=EOGenericRecord' or 'ERXGenericRecord', open the EOModel editor and assign custom Java class names to your entities.");
				log(logConsumer, result, "=========================================================");
			} else {
				log(logConsumer, result, "=========================================================");
				log(logConsumer, result, "RESULT: " + result.getGeneratedFiles().size() + " files generated successfully.");
				log(logConsumer, result, "=========================================================");
			}
		} catch (Exception ex) {
			LOG.error("EOGenerator failed", ex);
			result.setSuccess(false);
			log(logConsumer, result, "ERROR: EOGenerator failed with exception: " + ex.getMessage());
			StringWriter sw = new StringWriter();
			ex.printStackTrace(new PrintWriter(sw));
			log(logConsumer, result, sw.toString());
		}

		return result;
	}

	private static File findProjectOrContentRoot(Project project, File eogenFile) {
		if (project != null) {
			VirtualFile vf = LocalFileSystem.getInstance().findFileByIoFile(eogenFile);
			if (vf != null) {
				try {
					VirtualFile contentRoot = ProjectFileIndex.getInstance(project).getContentRootForFile(vf);
					if (contentRoot != null) {
						return new File(contentRoot.getPath());
					}
				} catch (Exception ignored) {
				}
			}
			if (project.getBasePath() != null) {
				return new File(project.getBasePath());
			}
		}
		return eogenFile.getParentFile();
	}

	public static File resolveModel(Project project, File workingDir, File projectRoot, String modelPath) {
		if (modelPath == null || modelPath.trim().isEmpty()) {
			return null;
		}

		File f = new File(modelPath);
		if (f.isAbsolute() && f.exists()) {
			return f;
		}

		// 1. Relative to workingDir
		if (workingDir != null) {
			File direct = new File(workingDir, modelPath);
			if (direct.exists()) return direct;
		}

		// 2. Relative to projectRoot / contentRoot
		if (projectRoot != null) {
			File fromRoot = new File(projectRoot, modelPath);
			if (fromRoot.exists()) return fromRoot;

			File inResources = new File(projectRoot, "Resources/" + modelPath);
			if (inResources.exists()) return inResources;

			File inMaven = new File(projectRoot, "src/main/resources/" + modelPath);
			if (inMaven.exists()) return inMaven;
		}

		// 3. Relative to project base path
		if (project != null && project.getBasePath() != null) {
			File base = new File(project.getBasePath());
			File fromBase = new File(base, modelPath);
			if (fromBase.exists()) return fromBase;

			File inBaseRes = new File(base, "Resources/" + modelPath);
			if (inBaseRes.exists()) return inBaseRes;

			File inBaseMaven = new File(base, "src/main/resources/" + modelPath);
			if (inBaseMaven.exists()) return inBaseMaven;
		}

		// 4. FilenameIndex across the project
		if (project != null && !project.isDisposed()) {
			String searchName = new File(modelPath).getName();
			if (!searchName.endsWith(".eomodeld")) {
				searchName += ".eomodeld";
			}
			final String finalSearchName = searchName;
			try {
				VirtualFile[] found = ApplicationManager.getApplication().runReadAction((com.intellij.openapi.util.Computable<VirtualFile[]>) () -> {
					Collection<VirtualFile> vfs = FilenameIndex.getVirtualFilesByName(
							finalSearchName,
							GlobalSearchScope.projectScope(project)
					);
					return vfs.toArray(VirtualFile.EMPTY_ARRAY);
				});
				if (found != null && found.length > 0) {
					return new File(found[0].getPath());
				}
			} catch (Exception ignored) {
			}
		}

		// Fallback
		return workingDir != null ? new File(workingDir, modelPath) : f;
	}

	public static File resolveDestination(Project project, File workingDir, File projectRoot, String path) {
		if (path == null || path.trim().isEmpty()) {
			if (projectRoot != null) {
				File sources = new File(projectRoot, "Sources");
				if (sources.exists()) return sources;
				File maven = new File(projectRoot, "src/main/java");
				if (maven.exists()) return maven;
				return sources;
			}
			return workingDir;
		}

		File f = new File(path);
		if (f.isAbsolute()) {
			return f;
		}

		// 1. If exists in projectRoot
		if (projectRoot != null) {
			File fromRoot = new File(projectRoot, path);
			if (fromRoot.exists()) return fromRoot;
		}

		// 2. If exists in project base
		if (project != null && project.getBasePath() != null) {
			File fromBase = new File(project.getBasePath(), path);
			if (fromBase.exists()) return fromBase;
		}

		// 3. If exists in workingDir
		if (workingDir != null) {
			File fromWorking = new File(workingDir, path);
			if (fromWorking.exists()) return fromWorking;
		}

		// 4. Default to projectRoot/path if creating new folder
		if (projectRoot != null) {
			return new File(projectRoot, path);
		}
		if (project != null && project.getBasePath() != null) {
			return new File(project.getBasePath(), path);
		}
		return new File(workingDir, path);
	}

	private static void generateEntity(VelocityEngine velocityEngine, EOModel model, EOEntity entity,
									   EOGeneratorModel eogenModel, File superclassDest, File subclassDest,
									   String superclassTemplateContent, String subclassTemplateContent,
									   GenerationResult result, Consumer<String> logConsumer) throws IOException {

		if (!entity.isGenerateSource()) {
			log(logConsumer, result, "  • Entity '" + entity.getName() + "': Skipped (generateSource is false in _EntityModeler)");
			return;
		}

		if (entity.isGenericRecord()) {
			log(logConsumer, result, "  • Entity '" + entity.getName() + "': Skipped (className is '" +
					(entity.getClassName() == null ? "null" : entity.getClassName()) + "', generic record has no custom class)");
			return;
		}

		String fullClassName = entity.getClassName();

		String packageName = "";
		String simpleClassName = fullClassName;
		int lastDot = fullClassName.lastIndexOf('.');
		if (lastDot > 0) {
			packageName = fullClassName.substring(0, lastDot);
			simpleClassName = fullClassName.substring(lastDot + 1);
		}

		// Calculate superclass package name
		String superclassPackageParam = eogenModel.getSuperclassPackage();
		String superclassPackageName;
		if (superclassPackageParam != null && !superclassPackageParam.trim().isEmpty()) {
			superclassPackageParam = superclassPackageParam.trim();
			if (superclassPackageParam.startsWith(".")) {
				superclassPackageParam = superclassPackageParam.substring(1);
			}
			if (!packageName.isEmpty()) {
				if (superclassPackageParam.startsWith(packageName + ".")) {
					superclassPackageName = superclassPackageParam;
				} else {
					superclassPackageName = packageName + "." + superclassPackageParam;
				}
			} else {
				superclassPackageName = superclassPackageParam;
			}
		} else {
			superclassPackageName = packageName;
		}

		String prefix = eogenModel.getPrefix() != null && !eogenModel.getPrefix().trim().isEmpty() ? eogenModel.getPrefix().trim() : "_";
		String extension = eogenModel.getExtension() != null && !eogenModel.getExtension().trim().isEmpty() ? eogenModel.getExtension().trim() : "java";
		String prefixClassNameWithoutPackage = prefix + simpleClassName;

		String prefixClassName;
		if (!superclassPackageName.isEmpty()) {
			prefixClassName = superclassPackageName + "." + prefixClassNameWithoutPackage;
		} else {
			prefixClassName = prefixClassNameWithoutPackage;
		}

		String prefixClassNameWithOptionalPackage;
		if (superclassPackageParam != null && !superclassPackageParam.trim().isEmpty()) {
			prefixClassNameWithOptionalPackage = prefixClassName;
		} else {
			prefixClassNameWithOptionalPackage = prefixClassNameWithoutPackage;
		}

		// Set transient helpers on entity for custom Velocity templates
		entity.setSuperclassPackageName(superclassPackageName);
		entity.setPrefixClassNameWithoutPackage(prefixClassNameWithoutPackage);
		entity.setPrefixClassNameWithOptionalPackage(prefixClassNameWithOptionalPackage);

		VelocityContext context = new VelocityContext();
		context.put("model", model);
		context.put("entity", entity);
		context.put("eogeneratorModel", eogenModel);
		context.put("className", fullClassName);
		context.put("packageName", packageName);
		context.put("superclassPackageName", superclassPackageName);
		context.put("classNameWithoutPackage", simpleClassName);
		context.put("classNameWithOptionalPackage", entity.getClassNameWithOptionalPackage());
		context.put("prefixClassNameWithoutPackage", prefixClassNameWithoutPackage);
		context.put("prefixClassName", prefixClassName);
		context.put("prefixClassNameWithOptionalPackage", prefixClassNameWithOptionalPackage);
		context.put("date", new Date());
		context.put("calendar", Calendar.getInstance());

		for (EODefine define : eogenModel.getDefines()) {
			context.put(define.getName(), define.getValue());
		}

		File superclassPackageDir = superclassDest;
		File subclassPackageDir = subclassDest;

		if (Boolean.TRUE.equals(eogenModel.isPackageDirs())) {
			if (!superclassPackageName.isEmpty()) {
				String superRelPath = superclassPackageName.replace('.', File.separatorChar);
				superclassPackageDir = new File(superclassDest, superRelPath);
			}
			if (!packageName.isEmpty()) {
				String subRelPath = packageName.replace('.', File.separatorChar);
				subclassPackageDir = new File(subclassDest, subRelPath);
			}
			superclassPackageDir.mkdirs();
			subclassPackageDir.mkdirs();
		}

		// 1. Superclass: always overwrite
		File superclassFile = new File(superclassPackageDir, prefixClassNameWithoutPackage + "." + extension);
		try (StringWriter sw = new StringWriter();
			 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(superclassFile), StandardCharsets.UTF_8))) {
			velocityEngine.evaluate(context, sw, "EOGenerator", superclassTemplateContent);
			bw.write(sw.toString());
			result.getGeneratedFiles().add(superclassFile.getName());
			log(logConsumer, result, "  • Entity '" + entity.getName() + "': Generated superclass -> " + superclassFile.getAbsolutePath());
		}

		// 2. Subclass: generate only if it doesn't already exist!
		File subclassFile = new File(subclassPackageDir, simpleClassName + "." + extension);
		if (!subclassFile.exists()) {
			try (StringWriter sw = new StringWriter();
				 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(subclassFile), StandardCharsets.UTF_8))) {
				velocityEngine.evaluate(context, sw, "EOGenerator", subclassTemplateContent);
				bw.write(sw.toString());
				result.getGeneratedFiles().add(subclassFile.getName());
				log(logConsumer, result, "  • Entity '" + entity.getName() + "': Generated subclass -> " + subclassFile.getAbsolutePath());
			}
		} else {
			log(logConsumer, result, "  • Entity '" + entity.getName() + "': Subclass " + subclassFile.getName() + " already exists (preserved).");
		}
	}

	private static void log(Consumer<String> logger, GenerationResult result, String message) {
		String line = "[" + LocalTime.now().format(TIME_FMT) + "] " + message;
		result.getMessages().add(line);
		LOG.info("[EOGenerator] " + message);
		if (logger != null) {
			logger.accept(line);
		}
	}

	public static class ResolvedTemplate {
		public final String name;
		public final String content;
		public final String sourceDescription;

		public ResolvedTemplate(String name, String content, String sourceDescription) {
			this.name = name;
			this.content = content;
			this.sourceDescription = sourceDescription;
		}
	}

	public static boolean isWonderProject(Project project, File projectRoot, EOGeneratorModel eogenModel) {
		if (eogenModel != null) {
			if (eogenModel.getDefineValue("wonder") != null || eogenModel.getDefineValue("Wonder") != null) {
				return true;
			}
			if (eogenModel.getDefineValue("ERXGenericRecord") != null) {
				return true;
			}
			if (eogenModel.getJavaTemplate() != null && eogenModel.getJavaTemplate().contains("Wonder")) {
				return true;
			}
			if (eogenModel.getSubclassJavaTemplate() != null && eogenModel.getSubclassJavaTemplate().contains("Wonder")) {
				return true;
			}
		}

		if (projectRoot != null && projectRoot.exists()) {
			File pom = new File(projectRoot, "pom.xml");
			if (pom.exists() && fileContains(pom, "wonder", "er.extensions", "ERExtensions")) {
				return true;
			}
			File gradle = new File(projectRoot, "build.gradle");
			if (gradle.exists() && fileContains(gradle, "wonder", "er.extensions", "ERExtensions")) {
				return true;
			}
			File gradleKts = new File(projectRoot, "build.gradle.kts");
			if (gradleKts.exists() && fileContains(gradleKts, "wonder", "er.extensions", "ERExtensions")) {
				return true;
			}
			File dotClasspath = new File(projectRoot, ".classpath");
			if (dotClasspath.exists() && fileContains(dotClasspath, "ERExtensions", "wonder")) {
				return true;
			}
			File templatesDir = new File(projectRoot, "Templates");
			if (new File(templatesDir, "_WonderEntity.java").exists()) {
				return true;
			}
			File resTemplatesDir = new File(projectRoot, "Resources/Templates");
			if (new File(resTemplatesDir, "_WonderEntity.java").exists()) {
				return true;
			}
		}

		if (project != null && project.getBasePath() != null) {
			File base = new File(project.getBasePath());
			File pom = new File(base, "pom.xml");
			if (pom.exists() && fileContains(pom, "wonder", "er.extensions", "ERExtensions")) {
				return true;
			}
			File gradle = new File(base, "build.gradle");
			if (gradle.exists() && fileContains(gradle, "wonder", "er.extensions", "ERExtensions")) {
				return true;
			}
		}

		return false;
	}

	private static boolean fileContains(File file, String... keywords) {
		try {
			String content = Files.readString(file.toPath());
			for (String kw : keywords) {
				if (content.toLowerCase().contains(kw.toLowerCase())) {
					return true;
				}
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	public static ResolvedTemplate resolveTemplate(File workingDir, File projectRoot, String templateDir,
												   String requestedTemplateName, boolean isSuperclass, boolean isWonder) {
		String templateName = requestedTemplateName;
		if (templateName == null || templateName.trim().isEmpty()) {
			if (isSuperclass) {
				templateName = isWonder ? "_WonderEntity.java" : "_Entity.java";
			} else {
				templateName = isWonder ? "WonderEntity.java" : "Entity.java";
			}
		}

		// 1. If explicit templateDir was provided
		if (templateDir != null && !templateDir.trim().isEmpty()) {
			File dir = new File(templateDir);
			if (!dir.isAbsolute()) {
				if (workingDir != null && new File(workingDir, templateDir).exists()) {
					dir = new File(workingDir, templateDir);
				} else if (projectRoot != null && new File(projectRoot, templateDir).exists()) {
					dir = new File(projectRoot, templateDir);
				}
			}
			File tFile = new File(dir, templateName);
			if (tFile.exists() && tFile.isFile()) {
				try {
					return new ResolvedTemplate(templateName, Files.readString(tFile.toPath(), StandardCharsets.UTF_8), tFile.getAbsolutePath());
				} catch (IOException ignored) {
				}
			}
		}

		// 2. Look in project Templates/ or Resources/Templates/
		if (projectRoot != null) {
			File t1 = new File(projectRoot, "Templates/" + templateName);
			if (t1.exists() && t1.isFile()) {
				try {
					return new ResolvedTemplate(templateName, Files.readString(t1.toPath(), StandardCharsets.UTF_8), t1.getAbsolutePath());
				} catch (IOException ignored) {
				}
			}
			File t2 = new File(projectRoot, "Resources/Templates/" + templateName);
			if (t2.exists() && t2.isFile()) {
				try {
					return new ResolvedTemplate(templateName, Files.readString(t2.toPath(), StandardCharsets.UTF_8), t2.getAbsolutePath());
				} catch (IOException ignored) {
				}
			}
		}
		if (workingDir != null) {
			File t3 = new File(workingDir, "Templates/" + templateName);
			if (t3.exists() && t3.isFile()) {
				try {
					return new ResolvedTemplate(templateName, Files.readString(t3.toPath(), StandardCharsets.UTF_8), t3.getAbsolutePath());
				} catch (IOException ignored) {
				}
			}
		}

		// 3. User's system WOLips folder (~/Library/Application Support/WOLips/EOGenerator/<templateName>)
		String userHome = System.getProperty("user.home");
		if (userHome != null) {
			File macAppSupport = new File(userHome, "Library/Application Support/WOLips/EOGenerator/" + templateName);
			if (macAppSupport.exists() && macAppSupport.isFile()) {
				try {
					return new ResolvedTemplate(templateName, Files.readString(macAppSupport.toPath(), StandardCharsets.UTF_8), macAppSupport.getAbsolutePath());
				} catch (IOException ignored) {
				}
			}
		}
		File sysAppSupport = new File("/Library/Application Support/WOLips/EOGenerator/" + templateName);
		if (sysAppSupport.exists() && sysAppSupport.isFile()) {
			try {
				return new ResolvedTemplate(templateName, Files.readString(sysAppSupport.toPath(), StandardCharsets.UTF_8), sysAppSupport.getAbsolutePath());
			} catch (IOException ignored) {
			}
		}

		// 4. Bundled classpath resource (/templates/eogen/<templateName>)
		String resourcePath = "/templates/eogen/" + templateName;
		String bundledContent = readClasspathResource(resourcePath);
		if (bundledContent != null) {
			return new ResolvedTemplate(templateName, bundledContent, "Plugin Classpath (" + resourcePath + ")");
		}

		// 5. If requestedTemplateName was not found in classpath, try fallback to default template name
		String fallbackName = isSuperclass ? (isWonder ? "_WonderEntity.java" : "_Entity.java") : (isWonder ? "WonderEntity.java" : "Entity.java");
		String fallbackPath = "/templates/eogen/" + fallbackName;
		String fallbackContent = readClasspathResource(fallbackPath);
		if (fallbackContent != null) {
			return new ResolvedTemplate(fallbackName, fallbackContent, "Plugin Classpath fallback (" + fallbackPath + ")");
		}

		// 6. Absolute last resort fallback (in case classpath resources failed)
		String codeFallback = isSuperclass ? getDefaultSuperclassTemplate() : getDefaultSubclassTemplate();
		return new ResolvedTemplate(templateName, codeFallback, "Built-in minimal code template");
	}

	private static String readClasspathResource(String resourcePath) {
		try (InputStream is = EOGeneratorRunner.class.getResourceAsStream(resourcePath)) {
			if (is != null) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buf = new byte[4096];
				int n;
				while ((n = is.read(buf)) > 0) {
					baos.write(buf, 0, n);
				}
				return baos.toString(StandardCharsets.UTF_8);
			}
		} catch (IOException ignored) {
		}
		return null;
	}

	private static GenerationResult runExternal(Project project, File workingDir, EOGeneratorModel eogenModel,
												GenerationResult result, Consumer<String> logConsumer) {
		try {
			List<String> command = new ArrayList<>();
			command.add(eogenModel.getEOGeneratorPath());
			CommandLineTokenizer tokenizer = new CommandLineTokenizer(eogenModel.writeToString(workingDir));
			while (tokenizer.hasMoreTokens()) {
				command.add(tokenizer.nextToken());
			}

			log(logConsumer, result, "Executing external command: " + String.join(" ", command));

			ProcessBuilder pb = new ProcessBuilder(command);
			if (workingDir != null) {
				pb.directory(workingDir);
			}
			pb.redirectErrorStream(true);
			Process proc = pb.start();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					log(logConsumer, result, line);
				}
			}

			int exitCode = proc.waitFor();
			result.setSuccess(exitCode == 0);
			log(logConsumer, result, "External process exited with code " + exitCode);
		} catch (Exception ex) {
			result.setSuccess(false);
			log(logConsumer, result, "External execution failed: " + ex.getMessage());
		}
		return result;
	}

	private static String getDefaultSuperclassTemplate() {
		return "// DO NOT EDIT. Make changes to ${classNameWithoutPackage}.java instead.\n" +
				"#if ($superclassPackageName && $superclassPackageName != \"\")\n" +
				"package ${superclassPackageName};\n" +
				"\n" +
				"#elseif ($packageName && $packageName != \"\")\n" +
				"package ${packageName};\n" +
				"\n" +
				"#end\n" +
				"import com.webobjects.eoaccess.*;\n" +
				"import com.webobjects.eocontrol.*;\n" +
				"import com.webobjects.foundation.*;\n" +
				"import java.math.*;\n" +
				"import java.util.*;\n" +
				"\n" +
				"@SuppressWarnings(\"all\")\n" +
				"public abstract class ${prefixClassNameWithoutPackage} extends #if ($EOGenericRecord)${EOGenericRecord}#else EOGenericRecord#end {\n" +
				"    public static final String ENTITY_NAME = \"${entity.name}\";\n" +
				"\n" +
				"    // Attributes\n" +
				"#foreach ($attribute in $entity.attributes)\n" +
				"    public static final String ${attribute.name.toUpperCase()}_KEY = \"${attribute.name}\";\n" +
				"#end\n" +
				"\n" +
				"    // Relationships\n" +
				"#foreach ($rel in $entity.relationships)\n" +
				"    public static final String ${rel.name.toUpperCase()}_KEY = \"${rel.name}\";\n" +
				"#end\n" +
				"\n" +
				"#foreach ($attribute in $entity.attributes)\n" +
				"    public ${attribute.formatJavaType} ${attribute.name}() {\n" +
				"        return (${attribute.formatJavaType}) storedValueForKey(\"${attribute.name}\");\n" +
				"    }\n" +
				"\n" +
				"    public void set${attribute.name.substring(0,1).toUpperCase()}${attribute.name.substring(1)}(${attribute.formatJavaType} value) {\n" +
				"        takeStoredValueForKey(value, \"${attribute.name}\");\n" +
				"    }\n" +
				"\n" +
				"#end\n" +
				"#foreach ($rel in $entity.relationships)\n" +
				"#if ($rel.toMany)\n" +
				"    public NSArray<${rel.destinationEntityName}> ${rel.name}() {\n" +
				"        return (NSArray<${rel.destinationEntityName}>) storedValueForKey(\"${rel.name}\");\n" +
				"    }\n" +
				"#else\n" +
				"    public ${rel.destinationEntityName} ${rel.name}() {\n" +
				"        return (${rel.destinationEntityName}) storedValueForKey(\"${rel.name}\");\n" +
				"    }\n" +
				"#end\n" +
				"#end\n" +
				"}\n";
	}

	private static String getDefaultSubclassTemplate() {
		return "#if ($packageName && $packageName != \"\")\n" +
				"package ${packageName};\n" +
				"\n" +
				"#end\n" +
				"#if ($superclassPackageName && $superclassPackageName != \"\" && $superclassPackageName != $packageName)\n" +
				"import ${prefixClassName};\n" +
				"\n" +
				"#end\n" +
				"public class ${classNameWithoutPackage} extends ${prefixClassNameWithoutPackage} {\n" +
				"}\n";
	}
}
