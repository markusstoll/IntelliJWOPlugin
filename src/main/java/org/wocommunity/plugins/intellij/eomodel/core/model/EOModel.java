package org.wocommunity.plugins.intellij.eomodel.core.model;

import org.wocommunity.plugins.intellij.eomodel.core.plist.PropertyListMap;
import org.wocommunity.plugins.intellij.eomodel.core.plist.PropertyListParserException;
import org.wocommunity.plugins.intellij.eomodel.core.plist.WOLPropertyListSerialization;

import java.io.File;
import java.io.IOException;
import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EOModel {
	public static final String DEFAULT_VERSION = "2.1";

	private String name;
	private File modelFolder;
	private String version = DEFAULT_VERSION;
	private String adaptorName = "None";
	private EOConnectionDictionary connectionDictionary = new EOConnectionDictionary();

	private final List<EOEntity> entities = new ArrayList<>();
	private final Set<String> deletedEntityNames = new HashSet<>();

	private final Map<Object, Object> userInfo = new PropertyListMap<>();
	private final Map<Object, Object> internalInfo = new PropertyListMap<>();
	private final Map<Object, Object> rawProperties = new PropertyListMap<>();

	private boolean dirty = false;

	public EOModel() {
	}

	public EOModel(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public File getModelFolder() {
		return modelFolder;
	}

	public void setModelFolder(File modelFolder) {
		this.modelFolder = modelFolder;
		if (modelFolder != null && (name == null || name.isEmpty())) {
			String folderName = modelFolder.getName();
			if (folderName.endsWith(".eomodeld")) {
				this.name = folderName.substring(0, folderName.length() - ".eomodeld".length());
			} else {
				this.name = folderName;
			}
		}
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getAdaptorName() {
		return adaptorName;
	}

	public void setAdaptorName(String adaptorName) {
		this.adaptorName = adaptorName;
	}

	public EOConnectionDictionary getConnectionDictionary() {
		return connectionDictionary;
	}

	public void setConnectionDictionary(EOConnectionDictionary connectionDictionary) {
		this.connectionDictionary = connectionDictionary != null ? connectionDictionary : new EOConnectionDictionary();
	}

	public List<EOEntity> getEntities() {
		return entities;
	}

	public EOEntity getEntity(String entityName) {
		if (entityName == null) return null;
		for (EOEntity entity : entities) {
			if (entityName.equals(entity.getName())) {
				return entity;
			}
		}
		return null;
	}

	public void addEntity(EOEntity entity) {
		if (entity != null && !entities.contains(entity)) {
			entity.setModel(this);
			entities.add(entity);
			deletedEntityNames.remove(entity.getName());
			setDirty(true);
		}
	}

	public void removeEntity(EOEntity entity) {
		if (entity != null && entities.remove(entity)) {
			if (entity.getName() != null) {
				deletedEntityNames.add(entity.getName());
			}
			setDirty(true);
		}
	}

	public EOEntity createEntity(String entityName) {
		EOEntity entity = new EOEntity(entityName);
		entity.setExternalName(entityName.toUpperCase(Locale.ROOT));
		entity.setClassName(entityName);
		addEntity(entity);
		return entity;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public Map<Object, Object> getUserInfo() {
		return userInfo;
	}

	public Map<Object, Object> getInternalInfo() {
		return internalInfo;
	}

	public Map<Object, Object> getRawProperties() {
		return rawProperties;
	}

	public void resolveModel() {
		for (EOEntity entity : entities) {
			entity.resolve(this);
		}
	}

	public void loadFromFolder(File folder) throws IOException, PropertyListParserException {
		if (folder == null || !folder.isDirectory()) {
			throw new IOException("Invalid model folder: " + folder);
		}
		setModelFolder(folder);

		File indexFile = new File(folder, "index.eomodeld");
		if (!indexFile.exists()) {
			throw new IOException("Missing index.eomodeld in " + folder);
		}

		Object plist = WOLPropertyListSerialization.propertyListFromFile(indexFile);
		if (!(plist instanceof Map)) {
			throw new PropertyListParserException("index.eomodeld root must be a map");
		}

		rawProperties.clear();
		rawProperties.putAll((Map) plist);
		EOModelMap m = new EOModelMap((Map) plist);

		Object v = m.get("EOModelVersion");
		if (v != null) {
			this.version = v.toString();
		}

		this.adaptorName = m.getString("adaptorName", true);
		if (this.adaptorName == null) {
			this.adaptorName = "None";
		}

		Map<?, ?> connMap = m.getMap("connectionDictionary");
		connectionDictionary.loadFromMap(connMap);

		entities.clear();
		deletedEntityNames.clear();

		List<?> entityRefs = m.getList("entities");
		if (entityRefs != null) {
			for (Object item : entityRefs) {
				if (item instanceof Map) {
					EOModelMap entRef = new EOModelMap((Map) item);
					String entityName = entRef.getString("name", true);
					if (entityName != null) {
						File entityFile = new File(folder, entityName + ".plist");
						if (entityFile.exists()) {
							EOEntity entity = new EOEntity();
							entity.setModel(this);
							entity.loadFromFile(entityFile);
							if (entity.getName() == null) {
								entity.setName(entityName);
							}
							entities.add(entity);
						}
					}
				}
			}
		}

		Map<?, ?> userInfoMap = m.getMap("userInfo");
		if (userInfoMap != null) {
			this.userInfo.clear();
			this.userInfo.putAll(userInfoMap);
		}

		Map<?, ?> internalInfoMap = m.getMap("internalInfo");
		if (internalInfoMap != null) {
			this.internalInfo.clear();
			this.internalInfo.putAll(internalInfoMap);
		}

		resolveModel();
		setDirty(false);
	}

	public EOModelMap toIndexMap() {
		EOModelMap indexMap = new EOModelMap(new PropertyListMap<>(rawProperties));
		indexMap.put("EOModelVersion", version != null ? version : DEFAULT_VERSION);
		indexMap.setString("adaptorName", adaptorName != null ? adaptorName : "None");
		indexMap.setMap("connectionDictionary", connectionDictionary.toMap());

		List<Map<String, Object>> entitiesList = new ArrayList<>();
		for (EOEntity entity : entities) {
			Map<String, Object> ref = new PropertyListMap<>();
			if (entity.getClassName() != null) {
				ref.put("className", entity.getClassName());
			}
			ref.put("name", entity.getName());
			entitiesList.add(ref);
		}
		indexMap.setList("entities", entitiesList);

		if (!userInfo.isEmpty()) {
			indexMap.setMap("userInfo", userInfo);
		} else {
			indexMap.remove("userInfo");
		}

		if (!internalInfo.isEmpty()) {
			indexMap.setMap("internalInfo", internalInfo);
		} else {
			indexMap.remove("internalInfo");
		}

		return indexMap;
	}

	public void save() throws IOException, PropertyListParserException {
		if (modelFolder == null) {
			throw new IOException("Cannot save EOModel: modelFolder is not set.");
		}
		saveToFolder(modelFolder);
	}

	public void saveToFolder(File folder) throws IOException, PropertyListParserException {
		if (!folder.exists() && !folder.mkdirs()) {
			throw new IOException("Could not create directory: " + folder);
		}
		setModelFolder(folder);

		// 1. Write index.eomodeld
		File indexFile = new File(folder, "index.eomodeld");
		EOModelMap indexMap = toIndexMap();
		WOLPropertyListSerialization.propertyListToFile("Entity Modeler", indexFile, indexMap.getBackingMap());

		// 2. Delete removed entities
		for (String delName : deletedEntityNames) {
			File delFile = new File(folder, delName + ".plist");
			if (delFile.exists()) {
				delFile.delete();
			}
			File fspecFile = new File(folder, delName + ".fspec");
			if (fspecFile.exists()) {
				fspecFile.delete();
			}
		}
		deletedEntityNames.clear();

		// 3. Save each entity
		for (EOEntity entity : entities) {
			File entityFile = new File(folder, entity.getName() + ".plist");
			entity.saveToFile(entityFile);
		}

		setDirty(false);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EOModel eoModel = (EOModel) o;
		return Objects.equals(name, eoModel.name);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name);
	}

	@Override
	public String toString() {
		return name != null ? name : super.toString();
	}
}

