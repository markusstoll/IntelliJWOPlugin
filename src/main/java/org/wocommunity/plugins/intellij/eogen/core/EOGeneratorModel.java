package org.wocommunity.plugins.intellij.eogen.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;

public class EOGeneratorModel {

	private String eogeneratorPath;
	private final List<String> models = new ArrayList<>();
	private final List<String> refModels = new ArrayList<>();
	private Boolean loadModelGroup = Boolean.FALSE;

	private String destination;
	private String subclassDestination;
	private String destination2;
	private String destination3;
	private String destination4;

	private String templateDir;
	private String javaTemplate;
	private String subclassJavaTemplate;
	private String javaTemplate2;
	private String javaTemplate3;
	private String javaTemplate4;

	private String filenameTemplate;
	private String prefix;
	private String extension = "java";
	private String superclassPackage;

	private Boolean packageDirs = Boolean.TRUE;
	private Boolean java = Boolean.TRUE;
	private Boolean javaClient = Boolean.FALSE;
	private Boolean javaClientCommon = Boolean.FALSE;
	private Boolean verbose = Boolean.FALSE;

	private final List<EODefine> defines = new ArrayList<>();
	private final List<String> customSettings = new ArrayList<>();

	private boolean dirty = false;

	public EOGeneratorModel() {
	}

	public String getEOGeneratorPath() {
		return eogeneratorPath;
	}

	public void setEOGeneratorPath(String eogeneratorPath) {
		this.eogeneratorPath = eogeneratorPath;
	}

	public List<String> getModels() {
		return models;
	}

	public void setModels(List<String> newModels) {
		this.models.clear();
		if (newModels != null) {
			this.models.addAll(newModels);
		}
		setDirty(true);
	}

	public void addModel(String modelPath) {
		if (modelPath != null && !models.contains(modelPath)) {
			models.add(modelPath);
			setDirty(true);
		}
	}

	public void removeModel(String modelPath) {
		if (models.remove(modelPath)) {
			setDirty(true);
		}
	}

	public List<String> getRefModels() {
		return refModels;
	}

	public void setRefModels(List<String> newRefModels) {
		this.refModels.clear();
		if (newRefModels != null) {
			this.refModels.addAll(newRefModels);
		}
		setDirty(true);
	}

	public void addRefModel(String refModelPath) {
		if (refModelPath != null && !refModels.contains(refModelPath)) {
			refModels.add(refModelPath);
			setDirty(true);
		}
	}

	public void removeRefModel(String refModelPath) {
		if (refModels.remove(refModelPath)) {
			setDirty(true);
		}
	}

	public Boolean isLoadModelGroup() {
		return loadModelGroup;
	}

	public void setLoadModelGroup(Boolean loadModelGroup) {
		this.loadModelGroup = loadModelGroup;
		setDirty(true);
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
		setDirty(true);
	}

	public String getSubclassDestination() {
		return subclassDestination;
	}

	public void setSubclassDestination(String subclassDestination) {
		this.subclassDestination = subclassDestination;
		setDirty(true);
	}

	public String getDestination2() {
		return destination2;
	}

	public void setDestination2(String destination2) {
		this.destination2 = destination2;
		setDirty(true);
	}

	public String getDestination3() {
		return destination3;
	}

	public void setDestination3(String destination3) {
		this.destination3 = destination3;
		setDirty(true);
	}

	public String getDestination4() {
		return destination4;
	}

	public void setDestination4(String destination4) {
		this.destination4 = destination4;
		setDirty(true);
	}

	public String getTemplateDir() {
		return templateDir;
	}

	public void setTemplateDir(String templateDir) {
		this.templateDir = templateDir;
		setDirty(true);
	}

	public String getJavaTemplate() {
		return javaTemplate;
	}

	public void setJavaTemplate(String javaTemplate) {
		this.javaTemplate = javaTemplate;
		setDirty(true);
	}

	public String getSubclassJavaTemplate() {
		return subclassJavaTemplate;
	}

	public void setSubclassJavaTemplate(String subclassJavaTemplate) {
		this.subclassJavaTemplate = subclassJavaTemplate;
		setDirty(true);
	}

	public String getJavaTemplate2() {
		return javaTemplate2;
	}

	public void setJavaTemplate2(String javaTemplate2) {
		this.javaTemplate2 = javaTemplate2;
		setDirty(true);
	}

	public String getJavaTemplate3() {
		return javaTemplate3;
	}

	public void setJavaTemplate3(String javaTemplate3) {
		this.javaTemplate3 = javaTemplate3;
		setDirty(true);
	}

	public String getJavaTemplate4() {
		return javaTemplate4;
	}

	public void setJavaTemplate4(String javaTemplate4) {
		this.javaTemplate4 = javaTemplate4;
		setDirty(true);
	}

	public String getFilenameTemplate() {
		return filenameTemplate;
	}

	public void setFilenameTemplate(String filenameTemplate) {
		this.filenameTemplate = filenameTemplate;
		setDirty(true);
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
		setDirty(true);
	}

	public String getExtension() {
		return extension != null ? extension : "java";
	}

	public void setExtension(String extension) {
		this.extension = extension;
		setDirty(true);
	}

	public String getSuperclassPackage() {
		return superclassPackage;
	}

	public void setSuperclassPackage(String superclassPackage) {
		this.superclassPackage = superclassPackage;
		setDirty(true);
	}

	public Boolean isPackageDirs() {
		return packageDirs;
	}

	public void setPackageDirs(Boolean packageDirs) {
		this.packageDirs = packageDirs;
		setDirty(true);
	}

	public Boolean isJava() {
		return java;
	}

	public void setJava(Boolean java) {
		this.java = java;
		setDirty(true);
	}

	public Boolean isJavaClient() {
		return javaClient;
	}

	public void setJavaClient(Boolean javaClient) {
		this.javaClient = javaClient;
		setDirty(true);
	}

	public Boolean isJavaClientCommon() {
		return javaClientCommon;
	}

	public void setJavaClientCommon(Boolean javaClientCommon) {
		this.javaClientCommon = javaClientCommon;
		setDirty(true);
	}

	public Boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(Boolean verbose) {
		this.verbose = verbose;
		setDirty(true);
	}

	public List<EODefine> getDefines() {
		return defines;
	}

	public void setDefines(List<EODefine> newDefines) {
		this.defines.clear();
		if (newDefines != null) {
			this.defines.addAll(newDefines);
		}
		setDirty(true);
	}

	public void addDefine(String name, String value) {
		for (EODefine d : defines) {
			if (Objects.equals(d.getName(), name)) {
				d.setValue(value);
				setDirty(true);
				return;
			}
		}
		defines.add(new EODefine(name, value));
		setDirty(true);
	}

	public void removeDefine(EODefine define) {
		if (defines.remove(define)) {
			setDirty(true);
		}
	}

	public String getDefineValue(String name) {
		for (EODefine d : defines) {
			if (Objects.equals(d.getName(), name)) {
				return d.getValue();
			}
		}
		return null;
	}

	public List<String> getCustomSettings() {
		return customSettings;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public void readFromFile(File file) throws IOException, ParseException {
		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append("\n");
			}
		}
		readFromString(sb.toString(), file.getParentFile());
		setDirty(false);
	}

	public void readFromString(String str) throws ParseException {
		readFromString(str, null);
	}

	public void writeToFile(File file) throws IOException {
		String text = writeToString(file.getParentFile());
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			writer.write(text);
			if (!text.endsWith("\n")) {
				writer.write("\n");
			}
		}
		setDirty(false);
	}

	public String writeToString() {
		return writeToString(null);
	}

	public void readFromString(String str, File workingDir) throws ParseException {
		models.clear();
		refModels.clear();
		defines.clear();
		customSettings.clear();

		CommandLineTokenizer tokenizer = new CommandLineTokenizer(str);
		while (tokenizer.hasMoreTokens()) {
			String token;
			try {
				token = tokenizer.nextToken();
			} catch (NoSuchElementException e) {
				break;
			}
			if (token == null) {
				break;
			}
			if (eogeneratorPath == null && !token.startsWith("-")) {
				eogeneratorPath = token;
			} else if (token.startsWith("-")) {
				if ("-destination".equalsIgnoreCase(token)) {
					destination = nextTokenValue(token, tokenizer);
				} else if ("-destination2".equalsIgnoreCase(token)) {
					destination2 = nextTokenValue(token, tokenizer);
				} else if ("-destination3".equalsIgnoreCase(token)) {
					destination3 = nextTokenValue(token, tokenizer);
				} else if ("-destination4".equalsIgnoreCase(token)) {
					destination4 = nextTokenValue(token, tokenizer);
				} else if ("-subclassDestination".equalsIgnoreCase(token)) {
					subclassDestination = nextTokenValue(token, tokenizer);
				} else if ("-extension".equalsIgnoreCase(token)) {
					extension = nextTokenValue(token, tokenizer);
				} else if ("-filenameTemplate".equalsIgnoreCase(token)) {
					filenameTemplate = nextTokenValue(token, tokenizer);
				} else if ("-java".equalsIgnoreCase(token)) {
					java = Boolean.TRUE;
				} else if ("-javaclient".equalsIgnoreCase(token)) {
					javaClient = Boolean.TRUE;
				} else if ("-javaclientcommon".equalsIgnoreCase(token)) {
					javaClientCommon = Boolean.TRUE;
				} else if ("-javaTemplate".equalsIgnoreCase(token)) {
					javaTemplate = nextTokenValue(token, tokenizer);
				} else if ("-javaTemplate2".equalsIgnoreCase(token)) {
					javaTemplate2 = nextTokenValue(token, tokenizer);
				} else if ("-javaTemplate3".equalsIgnoreCase(token)) {
					javaTemplate3 = nextTokenValue(token, tokenizer);
				} else if ("-javaTemplate4".equalsIgnoreCase(token)) {
					javaTemplate4 = nextTokenValue(token, tokenizer);
				} else if ("-subclassJavaTemplate".equalsIgnoreCase(token)) {
					subclassJavaTemplate = nextTokenValue(token, tokenizer);
				} else if ("-model".equalsIgnoreCase(token)) {
					models.add(nextTokenValue(token, tokenizer));
				} else if ("-refmodel".equalsIgnoreCase(token)) {
					refModels.add(nextTokenValue(token, tokenizer));
				} else if ("-templatedir".equalsIgnoreCase(token)) {
					templateDir = nextTokenValue(token, tokenizer);
				} else if ("-packagedirs".equalsIgnoreCase(token)) {
					packageDirs = Boolean.TRUE;
				} else if ("-prefix".equalsIgnoreCase(token)) {
					prefix = nextTokenValue(token, tokenizer);
				} else if ("-verbose".equalsIgnoreCase(token)) {
					verbose = Boolean.TRUE;
				} else if ("-loadModelGroup".equalsIgnoreCase(token)) {
					loadModelGroup = Boolean.TRUE;
				} else if ("-superclassPackage".equalsIgnoreCase(token)) {
					superclassPackage = nextTokenValue(token, tokenizer);
				} else if (token.startsWith("-define-")) {
					String name = token.substring("-define-".length());
					String value = nextTokenValue(token, tokenizer);
					defines.add(new EODefine(name, value));
				} else {
					customSettings.add(token);
				}
			} else {
				customSettings.add(token);
			}
		}
		setDirty(false);
	}

	private String nextTokenValue(String previousToken, CommandLineTokenizer tokenizer) throws ParseException {
		if (!tokenizer.hasMoreTokens()) {
			return "";
		}
		try {
			String token = tokenizer.nextToken();
			return token != null ? token.trim() : "";
		} catch (NoSuchElementException e) {
			return "";
		}
	}

	public String writeToString(File workingDirectory) {
		StringBuilder sb = new StringBuilder();

		if (eogeneratorPath != null && !eogeneratorPath.trim().isEmpty()) {
			sb.append(escape(eogeneratorPath, false));
		}

		append(sb, "-destination", destination);
		append(sb, "-destination2", destination2);
		append(sb, "-destination3", destination3);
		append(sb, "-destination4", destination4);
		append(sb, "-extension", extension);
		append(sb, "-filenameTemplate", filenameTemplate);
		append(sb, "-java", java);
		append(sb, "-javaclient", javaClient);
		append(sb, "-javaclientcommon", javaClientCommon);
		append(sb, "-javaTemplate", javaTemplate);
		append(sb, "-javaTemplate2", javaTemplate2);
		append(sb, "-javaTemplate3", javaTemplate3);
		append(sb, "-javaTemplate4", javaTemplate4);

		for (String model : models) {
			append(sb, "-model", model);
		}

		append(sb, "-packagedirs", packageDirs);
		append(sb, "-prefix", prefix);

		for (String refModel : refModels) {
			append(sb, "-refmodel", refModel);
		}

		append(sb, "-subclassDestination", subclassDestination);
		append(sb, "-subclassJavaTemplate", subclassJavaTemplate);
		append(sb, "-templatedir", templateDir);
		append(sb, "-verbose", verbose);
		append(sb, "-loadModelGroup", loadModelGroup);
		append(sb, "-superclassPackage", superclassPackage);

		for (EODefine define : defines) {
			if (define.getName() != null && !define.getName().isEmpty()) {
				append(sb, "-define-" + define.getName(), define.getValue());
			}
		}

		for (String custom : customSettings) {
			appendFlag(sb, custom);
		}

		return sb.toString();
	}

	private void appendFlag(StringBuilder sb, String flag) {
		if (flag != null && !flag.trim().isEmpty()) {
			if (sb.length() > 0) {
				sb.append(" \\\n");
			}
			sb.append(" ").append(flag.trim());
		}
	}

	private void append(StringBuilder sb, String name, Boolean value) {
		if (value != null && value) {
			if (sb.length() > 0) {
				sb.append(" \\\n");
			}
			sb.append(" ").append(name);
		}
	}

	private void append(StringBuilder sb, String name, String value) {
		if (value != null && !value.trim().isEmpty()) {
			if (sb.length() > 0) {
				sb.append(" \\\n");
			}
			sb.append(" ").append(name).append(" ").append(escape(value, true));
		}
	}

	private String escape(String value, boolean quotes) {
		if (value == null) return null;
		if (value.indexOf(' ') == -1 && !value.trim().isEmpty()) {
			return value;
		}
		if (quotes) {
			return "\"" + value + "\"";
		}
		return value.replace(" ", "\\ ");
	}
}
