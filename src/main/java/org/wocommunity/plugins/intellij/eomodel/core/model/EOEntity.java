package org.wocommunity.plugins.intellij.eomodel.core.model;

import org.wocommunity.plugins.intellij.eomodel.core.plist.PropertyListMap;
import org.wocommunity.plugins.intellij.eomodel.core.plist.PropertyListParserException;
import org.wocommunity.plugins.intellij.eomodel.core.plist.PropertyListSet;
import org.wocommunity.plugins.intellij.eomodel.core.plist.WOLPropertyListSerialization;

import java.io.File;
import java.io.IOException;
import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EOEntity {
	private EOModel model;
	private String name;
	private String className;
	private String externalName;
	private String parentEntityName;
	private EOEntity parentEntity;
	private String parentClassName;
	private boolean generateSource = true;

	private boolean abstractEntity = false;
	private boolean readOnly = false;

	private final List<EOAttribute> attributes = new ArrayList<>();
	private final List<EORelationship> relationships = new ArrayList<>();
	private final Map<String, Object> fetchSpecificationDictionary = new PropertyListMap<>();

	private final Map<Object, Object> userInfo = new PropertyListMap<>();
	private final Map<Object, Object> internalInfo = new PropertyListMap<>();
	private final Map<Object, Object> rawProperties = new PropertyListMap<>();

	public EOEntity() {
	}

	public EOEntity(String name) {
		this.name = name;
	}

	public EOModel getModel() {
		return model;
	}

	public void setModel(EOModel model) {
		this.model = model;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getPackageName() {
		if (className == null) return "";
		int lastDot = className.lastIndexOf('.');
		return lastDot > 0 ? className.substring(0, lastDot) : "";
	}

	public String getClassNameWithoutPackage() {
		if (className == null) return name;
		int lastDot = className.lastIndexOf('.');
		return lastDot > 0 ? className.substring(lastDot + 1) : className;
	}

	public String getClassNameWithDefault() {
		if (className != null && !className.trim().isEmpty()) {
			return className;
		}
		return "com.webobjects.eocontrol.EOGenericRecord";
	}

	public String getClassNameWithOptionalPackage() {
		if (superclassPackageName != null && !superclassPackageName.equals(getPackageName())) {
			return getClassName();
		}
		return getClassNameWithoutPackage();
	}

	public boolean isParentClassNameSet() {
		return parentClassName != null && !parentClassName.trim().isEmpty();
	}

	public String getParentClassName() {
		return parentClassName;
	}

	public void setParentClassName(String parentClassName) {
		this.parentClassName = parentClassName;
		if (parentClassName != null && !parentClassName.trim().isEmpty()) {
			Map<Object, Object> emMap = (Map<Object, Object>) userInfo.computeIfAbsent("_EntityModeler", k -> new PropertyListMap<>());
			emMap.put("parentClassName", parentClassName);
		} else if (userInfo.get("_EntityModeler") instanceof Map<?, ?> emMap) {
			((Map<Object, Object>) emMap).remove("parentClassName");
		}
	}

	public boolean isGenerateSource() {
		return generateSource;
	}

	public void setGenerateSource(boolean generateSource) {
		this.generateSource = generateSource;
		if (!generateSource) {
			Map<Object, Object> emMap = (Map<Object, Object>) userInfo.computeIfAbsent("_EntityModeler", k -> new PropertyListMap<>());
			emMap.put("generateSource", "NO");
		} else if (userInfo.get("_EntityModeler") instanceof Map<?, ?> emMap) {
			((Map<Object, Object>) emMap).remove("generateSource");
		}
	}

	public boolean isParentSet() {
		return getParent() != null;
	}

	public EOEntity getParent() {
		if (parentEntity != null) {
			return parentEntity;
		}
		if (model != null && parentEntityName != null) {
			return model.getEntity(parentEntityName);
		}
		return null;
	}

	public boolean isPartialEntitySet() {
		return false;
	}

	public EOEntity getPartialEntity() {
		return null;
	}

	public boolean isInherited() {
		return isParentSet();
	}

	public boolean isGenericRecord() {
		if (className == null || className.trim().isEmpty()) {
			return true;
		}
		return className.endsWith("GenericRecord") && (name == null || !name.endsWith("GenericRecord"));
	}

	public String getPluralName() {
		if (name == null || name.isEmpty()) return "";
		if (name.endsWith("y") && !name.endsWith("ay") && !name.endsWith("ey") && !name.endsWith("oy") && !name.endsWith("uy")) {
			return name.substring(0, name.length() - 1) + "ies";
		}
		if (name.endsWith("s") || name.endsWith("x") || name.endsWith("z") || name.endsWith("ch") || name.endsWith("sh")) {
			return name + "es";
		}
		return name + "s";
	}

	public Set<String> getRestrictingQualifierKeys() {
		return Collections.emptySet();
	}

	public List<EORelationship> getSortedClassToOneRelationships() {
		List<EORelationship> list = new ArrayList<>();
		for (EORelationship rel : getRelationships()) {
			if (!rel.isToMany() && rel.isClassProperty()) {
				list.add(rel);
			}
		}
		list.sort(Comparator.comparing(EORelationship::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(EORelationship::getName));
		return list;
	}

	public List<EORelationship> getSortedClassToManyRelationships() {
		List<EORelationship> list = new ArrayList<>();
		for (EORelationship rel : getRelationships()) {
			if (rel.isToMany() && rel.isClassProperty()) {
				list.add(rel);
			}
		}
		list.sort(Comparator.comparing(EORelationship::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(EORelationship::getName));
		return list;
	}

	public List<EORelationship> getSortedToOneRelationships() {
		List<EORelationship> list = new ArrayList<>();
		for (EORelationship rel : getRelationships()) {
			if (!rel.isToMany()) {
				list.add(rel);
			}
		}
		list.sort(Comparator.comparing(EORelationship::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(EORelationship::getName));
		return list;
	}

	public List<EORelationship> getSortedToManyRelationships() {
		List<EORelationship> list = new ArrayList<>();
		for (EORelationship rel : getRelationships()) {
			if (rel.isToMany()) {
				list.add(rel);
			}
		}
		list.sort(Comparator.comparing(EORelationship::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(EORelationship::getName));
		return list;
	}

	public List<Object> getSortedFetchSpecs() {
		return Collections.emptyList();
	}

	private transient String superclassPackageName;
	private transient String prefixClassNameWithoutPackage;
	private transient String prefixClassNameWithOptionalPackage;

	public String getSuperclassPackageName() {
		return superclassPackageName != null ? superclassPackageName : getPackageName();
	}

	public void setSuperclassPackageName(String superclassPackageName) {
		this.superclassPackageName = superclassPackageName;
	}

	public String getPrefixClassNameWithoutPackage() {
		return prefixClassNameWithoutPackage != null ? prefixClassNameWithoutPackage : "_" + getClassNameWithoutPackage();
	}

	public void setPrefixClassNameWithoutPackage(String prefixClassNameWithoutPackage) {
		this.prefixClassNameWithoutPackage = prefixClassNameWithoutPackage;
	}

	public String getPrefixClassNameWithOptionalPackage() {
		return prefixClassNameWithOptionalPackage != null ? prefixClassNameWithOptionalPackage : getPrefixClassNameWithoutPackage();
	}

	public void setPrefixClassNameWithOptionalPackage(String prefixClassNameWithOptionalPackage) {
		this.prefixClassNameWithOptionalPackage = prefixClassNameWithOptionalPackage;
	}

	public List<EOAttribute> getClassAttributes() {
		List<EOAttribute> list = new ArrayList<>();
		for (EOAttribute attr : getAttributes()) {
			if (attr.isClassProperty()) {
				list.add(attr);
			}
		}
		return list;
	}

	public List<EOAttribute> getSortedAttributes() {
		List<EOAttribute> list = new ArrayList<>(getAttributes());
		list.sort(Comparator.comparing(EOAttribute::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(EOAttribute::getName));
		return list;
	}

	public List<EOAttribute> getSortedClassAttributes() {
		List<EOAttribute> list = new ArrayList<>(getClassAttributes());
		list.sort(Comparator.comparing(EOAttribute::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(EOAttribute::getName));
		return list;
	}

	public List<EOAttribute> getNonClassAttributes() {
		List<EOAttribute> list = new ArrayList<>();
		for (EOAttribute attr : getAttributes()) {
			if (!attr.isClassProperty()) {
				list.add(attr);
			}
		}
		return list;
	}

	public List<EORelationship> getSortedRelationships() {
		List<EORelationship> list = new ArrayList<>(getRelationships());
		list.sort(Comparator.comparing(EORelationship::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(EORelationship::getName));
		return list;
	}

	public List<EORelationship> getClassRelationships() {
		List<EORelationship> list = new ArrayList<>();
		for (EORelationship rel : getRelationships()) {
			if (rel.isClassProperty()) {
				list.add(rel);
			}
		}
		return list;
	}

	public List<EORelationship> getSortedClassRelationships() {
		List<EORelationship> list = new ArrayList<>(getClassRelationships());
		list.sort(Comparator.comparing(EORelationship::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(EORelationship::getName));
		return list;
	}

	public List<EORelationship> getNonClassRelationships() {
		List<EORelationship> list = new ArrayList<>();
		for (EORelationship rel : getRelationships()) {
			if (!rel.isClassProperty()) {
				list.add(rel);
			}
		}
		return list;
	}

	public String getExternalName() {
		return externalName != null ? externalName : name;
	}

	public void setExternalName(String externalName) {
		this.externalName = externalName;
	}

	public String getParentEntityName() {
		return parentEntityName;
	}

	public void setParentEntityName(String parentEntityName) {
		this.parentEntityName = parentEntityName;
	}

	public EOEntity getParentEntity() {
		return parentEntity;
	}

	public void setParentEntity(EOEntity parentEntity) {
		this.parentEntity = parentEntity;
		if (parentEntity != null) {
			this.parentEntityName = parentEntity.getName();
		}
	}

	public boolean isAbstractEntity() {
		return abstractEntity;
	}

	public void setAbstractEntity(boolean abstractEntity) {
		this.abstractEntity = abstractEntity;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public List<EOAttribute> getAttributes() {
		return attributes;
	}

	public EOAttribute getAttribute(String attributeName) {
		if (attributeName == null) return null;
		for (EOAttribute attr : attributes) {
			if (attributeName.equals(attr.getName())) {
				return attr;
			}
		}
		return null;
	}

	public void addAttribute(EOAttribute attribute) {
		if (attribute != null && !attributes.contains(attribute)) {
			attribute.setEntity(this);
			attributes.add(attribute);
		}
	}

	public void removeAttribute(EOAttribute attribute) {
		attributes.remove(attribute);
	}

	public List<EORelationship> getRelationships() {
		return relationships;
	}

	public EORelationship getRelationship(String relationshipName) {
		if (relationshipName == null) return null;
		for (EORelationship rel : relationships) {
			if (relationshipName.equals(rel.getName())) {
				return rel;
			}
		}
		return null;
	}

	public void addRelationship(EORelationship relationship) {
		if (relationship != null && !relationships.contains(relationship)) {
			relationship.setEntity(this);
			relationships.add(relationship);
		}
	}

	public void removeRelationship(EORelationship relationship) {
		relationships.remove(relationship);
	}

	public List<EOAttribute> getPrimaryKeys() {
		List<EOAttribute> pks = new ArrayList<>();
		for (EOAttribute attr : attributes) {
			if (attr.isPrimaryKey()) {
				pks.add(attr);
			}
		}
		return pks;
	}

	public List<EOAttribute> getClassPropertyAttributes() {
		List<EOAttribute> props = new ArrayList<>();
		for (EOAttribute attr : attributes) {
			if (attr.isClassProperty()) {
				props.add(attr);
			}
		}
		return props;
	}

	public List<EORelationship> getClassPropertyRelationships() {
		List<EORelationship> props = new ArrayList<>();
		for (EORelationship rel : relationships) {
			if (rel.isClassProperty()) {
				props.add(rel);
			}
		}
		return props;
	}

	public Map<String, Object> getFetchSpecificationDictionary() {
		return fetchSpecificationDictionary;
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

	public void resolve(EOModel eoModel) {
		if (eoModel != null && parentEntityName != null) {
			this.parentEntity = eoModel.getEntity(parentEntityName);
		}
		for (EORelationship rel : relationships) {
			rel.resolve(eoModel);
		}
	}

	public void loadFromMap(Map<?, ?> map) {
		rawProperties.clear();
		if (map == null) return;
		rawProperties.putAll(map);
		EOModelMap m = new EOModelMap(map);

		this.name = m.getString("name", true);
		this.className = m.getString("className", true);
		this.externalName = m.getString("externalName", true);
		this.parentEntityName = m.getString("parent", true);

		this.abstractEntity = m.getBoolean("isAbstractEntity", false);
		this.readOnly = m.getBoolean("isReadOnly", false);

		Set<String> pkNames = new HashSet<>();
		List<?> rawPks = m.getList("primaryKeyAttributes");
		if (rawPks != null) {
			for (Object pk : rawPks) {
				if (pk != null) pkNames.add(pk.toString());
			}
		}

		Set<String> classPropNames = new HashSet<>();
		List<?> rawClassProps = m.getList("classProperties");
		if (rawClassProps != null) {
			for (Object cp : rawClassProps) {
				if (cp != null) classPropNames.add(cp.toString());
			}
		}

		Set<String> lockingNames = new HashSet<>();
		List<?> rawLocking = m.getList("attributesUsedForLocking");
		if (rawLocking != null) {
			for (Object lk : rawLocking) {
				if (lk != null) lockingNames.add(lk.toString());
			}
		}

		// Load attributes
		attributes.clear();
		List<?> rawAttrs = m.getList("attributes");
		if (rawAttrs != null) {
			for (Object item : rawAttrs) {
				if (item instanceof Map) {
					EOAttribute attr = new EOAttribute();
					attr.setEntity(this);
					attr.loadFromMap((Map<?, ?>) item);
					attr.setPrimaryKey(pkNames.contains(attr.getName()));
					attr.setClassProperty(classPropNames.contains(attr.getName()));
					attr.setUsedForLocking(lockingNames.contains(attr.getName()));
					attributes.add(attr);
				}
			}
		}

		// Load relationships
		relationships.clear();
		List<?> rawRels = m.getList("relationships");
		if (rawRels != null) {
			for (Object item : rawRels) {
				if (item instanceof Map) {
					EORelationship rel = new EORelationship();
					rel.setEntity(this);
					rel.loadFromMap((Map<?, ?>) item);
					rel.setClassProperty(classPropNames.contains(rel.getName()));
					relationships.add(rel);
				}
			}
		}

		Map<?, ?> fspecs = m.getMap("fetchSpecificationDictionary");
		fetchSpecificationDictionary.clear();
		if (fspecs != null) {
			fetchSpecificationDictionary.putAll((Map) fspecs);
		}

		this.parentClassName = null;
		this.generateSource = true;
		Map<?, ?> userInfoMap = m.getMap("userInfo");
		if (userInfoMap != null) {
			this.userInfo.clear();
			this.userInfo.putAll(userInfoMap);

			Object entityModelerObj = this.userInfo.get("_EntityModeler");
			if (entityModelerObj instanceof Map<?, ?> emMap) {
				Object pcn = emMap.get("parentClassName");
				if (pcn != null && !pcn.toString().trim().isEmpty()) {
					this.parentClassName = pcn.toString().trim();
				}
				Object gs = emMap.get("generateSource");
				if (gs instanceof Boolean b) {
					this.generateSource = b;
				} else if (gs != null) {
					String gsStr = gs.toString().trim();
					this.generateSource = !"NO".equalsIgnoreCase(gsStr) && !"false".equalsIgnoreCase(gsStr) && !"0".equals(gsStr);
				}
			}
		} else {
			this.userInfo.clear();
		}

		Map<?, ?> internalInfoMap = m.getMap("internalInfo");
		if (internalInfoMap != null) {
			this.internalInfo.clear();
			this.internalInfo.putAll(internalInfoMap);
		}
	}

	public EOModelMap toMap() {
		EOModelMap map = new EOModelMap(new PropertyListMap<>(rawProperties));
		map.setString("name", name, true);
		map.setString("className", className, true);
		map.setString("externalName", externalName, true);
		map.setString("parent", parentEntityName, true);

		map.setBoolean("isAbstractEntity", abstractEntity, EOModelMap.BooleanStyle.Y_N_OPTIONAL_DEFAULT_NO);
		map.setBoolean("isReadOnly", readOnly, EOModelMap.BooleanStyle.Y_N_OPTIONAL_DEFAULT_NO);

		List<Map<String, Object>> attrsList = new ArrayList<>();
		List<String> pkNames = new ArrayList<>();
		List<String> classPropNames = new ArrayList<>();
		List<String> lockingNames = new ArrayList<>();

		for (EOAttribute attr : attributes) {
			attrsList.add((Map) attr.toMap().getBackingMap());
			if (attr.isPrimaryKey()) {
				pkNames.add(attr.getName());
			}
			if (attr.isClassProperty()) {
				classPropNames.add(attr.getName());
			}
			if (attr.isUsedForLocking()) {
				lockingNames.add(attr.getName());
			}
		}
		map.setList("attributes", attrsList);
		map.setList("primaryKeyAttributes", pkNames);
		map.setList("attributesUsedForLocking", lockingNames);

		List<Map<String, Object>> relsList = new ArrayList<>();
		for (EORelationship rel : relationships) {
			relsList.add((Map) rel.toMap().getBackingMap());
			if (rel.isClassProperty()) {
				classPropNames.add(rel.getName());
			}
		}
		map.setList("relationships", relsList);
		map.setList("classProperties", classPropNames);

		if (fetchSpecificationDictionary != null && !fetchSpecificationDictionary.isEmpty()) {
			map.setMap("fetchSpecificationDictionary", fetchSpecificationDictionary);
		} else {
			map.put("fetchSpecificationDictionary", new PropertyListMap<>());
		}

		if (parentClassName != null && !parentClassName.trim().isEmpty()) {
			Map<Object, Object> emMap = (Map<Object, Object>) userInfo.computeIfAbsent("_EntityModeler", k -> new PropertyListMap<>());
			emMap.put("parentClassName", parentClassName);
		} else if (userInfo.get("_EntityModeler") instanceof Map<?, ?> emMap) {
			((Map<Object, Object>) emMap).remove("parentClassName");
		}

		if (!generateSource) {
			Map<Object, Object> emMap = (Map<Object, Object>) userInfo.computeIfAbsent("_EntityModeler", k -> new PropertyListMap<>());
			emMap.put("generateSource", "NO");
		} else if (userInfo.get("_EntityModeler") instanceof Map<?, ?> emMap) {
			((Map<Object, Object>) emMap).remove("generateSource");
		}

		if (!userInfo.isEmpty()) {
			map.setMap("userInfo", userInfo);
		} else {
			map.remove("userInfo");
		}

		if (!internalInfo.isEmpty()) {
			map.setMap("internalInfo", internalInfo);
		} else {
			map.remove("internalInfo");
		}

		return map;
	}

	public void loadFromFile(File entityFile) throws IOException, PropertyListParserException {
		Object plist = WOLPropertyListSerialization.propertyListFromFile(entityFile);
		if (plist instanceof Map) {
			loadFromMap((Map<?, ?>) plist);
		} else {
			throw new PropertyListParserException("Expected map in entity file: " + entityFile);
		}
	}

	public void saveToFile(File entityFile) throws IOException, PropertyListParserException {
		EOModelMap map = toMap();
		WOLPropertyListSerialization.propertyListToFile("Entity Modeler", entityFile, map.getBackingMap());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EOEntity eoEntity = (EOEntity) o;
		return Objects.equals(name, eoEntity.name);
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

