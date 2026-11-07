package org.wocommunity.plugins.intellij.eogen.core;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOEntity;
import org.wocommunity.plugins.intellij.eomodel.core.model.EOModel;
import org.wocommunity.plugins.intellij.eomodel.core.model.EORelationship;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EOGeneratorRunner {
	private static final Logger LOG = Logger.getInstance(EOGeneratorRunner.class);

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
		GenerationResult result = new GenerationResult();
		File workingDir = eogenFile.getParentFile();
		if (workingDir == null && project != null && project.getBasePath() != null) {
			workingDir = new File(project.getBasePath());
		}

		// 1. Check for external executable
		String extPath = eogenModel.getEOGeneratorPath();
		if (extPath != null && !extPath.trim().isEmpty()) {
			File extExe = new File(extPath);
			if (extExe.exists() && extExe.canExecute()) {
				return runExternal(project, workingDir, eogenModel, result);
			}
		}

		// 2. Built-in Generation
		try {
			File superclassDest = resolveDirectory(workingDir, eogenModel.getDestination());
			File subclassDest = resolveDirectory(workingDir, eogenModel.getSubclassDestination());

			if (!superclassDest.exists() && !superclassDest.mkdirs()) {
				throw new IOException("Failed to create destination directory: " + superclassDest);
			}
			if (!subclassDest.exists() && !subclassDest.mkdirs()) {
				throw new IOException("Failed to create subclass destination directory: " + subclassDest);
			}

			// Initialize Velocity Engine
			VelocityEngine velocityEngine = new VelocityEngine();
			velocityEngine.setProperty("input.encoding", "UTF-8");
			velocityEngine.setProperty("output.encoding", "UTF-8");
			velocityEngine.init();

			String superclassTemplateContent = loadTemplate(workingDir, eogenModel.getTemplateDir(), eogenModel.getJavaTemplate(), true);
			String subclassTemplateContent = loadTemplate(workingDir, eogenModel.getTemplateDir(), eogenModel.getSubclassJavaTemplate(), false);

			// Load each model
			for (String modelPath : eogenModel.getModels()) {
				File modelFolder = resolveDirectory(workingDir, modelPath);
				if (!modelFolder.exists()) {
					result.getMessages().add("Warning: Model folder does not exist: " + modelFolder.getPath());
					continue;
				}

				EOModel eoModel = new EOModel();
				eoModel.loadFromFolder(modelFolder);

				for (EOEntity entity : eoModel.getEntities()) {
					if (indicator != null) {
						indicator.setText("Generating " + entity.getName() + "...");
					}

					generateEntity(velocityEngine, eoModel, entity, eogenModel, superclassDest, subclassDest,
							superclassTemplateContent, subclassTemplateContent, result);
				}
			}

			// Refresh VFS
			if (com.intellij.openapi.application.ApplicationManager.getApplication() != null) {
				VirtualFile superclassVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(superclassDest);
				if (superclassVf != null) {
					superclassVf.refresh(true, true);
				}
				VirtualFile subclassVf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(subclassDest);
				if (subclassVf != null) {
					subclassVf.refresh(true, true);
				}
			}

			result.getMessages().add("Generated " + result.getGeneratedFiles().size() + " files successfully.");
		} catch (Exception ex) {
			LOG.error("EOGenerator failed", ex);
			result.setSuccess(false);
			result.getMessages().add("Error: " + ex.getMessage());
		}

		return result;
	}

	private static void generateEntity(VelocityEngine velocityEngine, EOModel model, EOEntity entity,
									   EOGeneratorModel eogenModel, File superclassDest, File subclassDest,
									   String superclassTemplateContent, String subclassTemplateContent,
									   GenerationResult result) throws IOException {

		String fullClassName = entity.getClassName();
		if (fullClassName == null || fullClassName.trim().isEmpty() || fullClassName.equals("EOGenericRecord")) {
			return; // Don't generate for raw generic record
		}

		String packageName = "";
		String simpleClassName = fullClassName;
		int lastDot = fullClassName.lastIndexOf('.');
		if (lastDot > 0) {
			packageName = fullClassName.substring(0, lastDot);
			simpleClassName = fullClassName.substring(lastDot + 1);
		}

		String prefix = eogenModel.getPrefix() != null ? eogenModel.getPrefix() : "_";
		String prefixClassName = prefix + simpleClassName;

		VelocityContext context = new VelocityContext();
		context.put("model", model);
		context.put("entity", entity);
		context.put("className", fullClassName);
		context.put("packageName", packageName);
		context.put("classNameWithoutPackage", simpleClassName);
		context.put("prefixClassNameWithoutPackage", prefixClassName);
		context.put("prefixClassName", (packageName.isEmpty() ? "" : packageName + ".") + prefixClassName);

		for (EODefine define : eogenModel.getDefines()) {
			context.put(define.getName(), define.getValue());
		}

		File superclassPackageDir = superclassDest;
		File subclassPackageDir = subclassDest;

		if (Boolean.TRUE.equals(eogenModel.isPackageDirs()) && !packageName.isEmpty()) {
			String relPath = packageName.replace('.', File.separatorChar);
			superclassPackageDir = new File(superclassDest, relPath);
			subclassPackageDir = new File(subclassDest, relPath);
			superclassPackageDir.mkdirs();
			subclassPackageDir.mkdirs();
		}

		// 1. Superclass: always overwrite
		File superclassFile = new File(superclassPackageDir, prefixClassName + "." + eogenModel.getExtension());
		try (StringWriter sw = new StringWriter();
			 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(superclassFile), StandardCharsets.UTF_8))) {
			velocityEngine.evaluate(context, sw, "EOGenerator", superclassTemplateContent);
			bw.write(sw.toString());
			result.getGeneratedFiles().add(superclassFile.getName());
		}

		// 2. Subclass: generate only if it doesn't already exist!
		File subclassFile = new File(subclassPackageDir, simpleClassName + "." + eogenModel.getExtension());
		if (!subclassFile.exists()) {
			try (StringWriter sw = new StringWriter();
				 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(subclassFile), StandardCharsets.UTF_8))) {
				velocityEngine.evaluate(context, sw, "EOGenerator", subclassTemplateContent);
				bw.write(sw.toString());
				result.getGeneratedFiles().add(subclassFile.getName());
			}
		}
	}

	private static String loadTemplate(File workingDir, String templateDir, String templateName, boolean isSuperclass) {
		if (templateDir != null && !templateDir.trim().isEmpty()) {
			File dir = resolveDirectory(workingDir, templateDir);
			File tFile = new File(dir, templateName);
			if (tFile.exists()) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tFile), StandardCharsets.UTF_8))) {
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						sb.append(line).append("\n");
					}
					return sb.toString();
				} catch (IOException ignored) {
				}
			}
		}

		// Default built-in template
		if (isSuperclass) {
			return getDefaultSuperclassTemplate();
		} else {
			return getDefaultSubclassTemplate();
		}
	}

	private static File resolveDirectory(File workingDir, String path) {
		if (path == null || path.trim().isEmpty()) {
			return workingDir;
		}
		File f = new File(path);
		if (f.isAbsolute()) {
			return f;
		}
		return new File(workingDir, path);
	}

	private static GenerationResult runExternal(Project project, File workingDir, EOGeneratorModel eogenModel, GenerationResult result) {
		try {
			List<String> command = new ArrayList<>();
			command.add(eogenModel.getEOGeneratorPath());
			CommandLineTokenizer tokenizer = new CommandLineTokenizer(eogenModel.writeToString(workingDir));
			while (tokenizer.hasMoreTokens()) {
				command.add(tokenizer.nextToken());
			}

			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(workingDir);
			pb.redirectErrorStream(true);
			Process proc = pb.start();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					result.getMessages().add(line);
				}
			}

			int exitCode = proc.waitFor();
			result.setSuccess(exitCode == 0);
		} catch (Exception ex) {
			result.setSuccess(false);
			result.getMessages().add("External execution failed: " + ex.getMessage());
		}
		return result;
	}

	private static String getDefaultSuperclassTemplate() {
		return "// DO NOT EDIT. Make changes to ${classNameWithoutPackage}.java instead.\n" +
				"#if ($packageName && $packageName != \"\")\n" +
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
				"\n" +
				"    public void set${rel.name.substring(0,1).toUpperCase()}${rel.name.substring(1)}(${rel.destinationEntityName} value) {\n" +
				"        takeStoredValueForKey(value, \"${rel.name}\");\n" +
				"    }\n" +
				"#end\n" +
				"\n" +
				"#end\n" +
				"}\n";
	}

	private static String getDefaultSubclassTemplate() {
		return "#if ($packageName && $packageName != \"\")\n" +
				"package ${packageName};\n" +
				"\n" +
				"#end\n" +
				"public class ${classNameWithoutPackage} extends ${prefixClassNameWithoutPackage} {\n" +
				"}\n";
	}
}
