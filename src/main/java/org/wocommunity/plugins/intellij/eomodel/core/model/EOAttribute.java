package org.wocommunity.plugins.intellij.eomodel.core.model;

import org.wocommunity.plugins.intellij.eomodel.core.plist.PropertyListMap;

import java.util.Map;
import java.util.Objects;

public class EOAttribute {
	private EOEntity entity;
	private String name;
	private String columnName;
	private String externalType;
	private String valueClassName;
	private String valueType;
	private String prototypeName;
	private String definition;

	private boolean allowsNull = false;
	private Integer width;
	private Integer precision;
	private Integer scale;
	private boolean readOnly = false;

	// Flags governed at the entity level in EOModel plist
	private boolean primaryKey = false;
	private boolean classProperty = true;
	private boolean usedForLocking = true;
	private boolean clientClassProperty = false;

	private final Map<Object, Object> userInfo = new PropertyListMap<>();
	private final Map<Object, Object> internalInfo = new PropertyListMap<>();
	private final Map<Object, Object> rawProperties = new PropertyListMap<>();

	public EOAttribute() {
	}

	public EOAttribute(String name) {
		this.name = name;
	}

	public EOEntity getEntity() {
		return entity;
	}

	public void setEntity(EOEntity entity) {
		this.entity = entity;
	}

	public String getName() {
		return name;
	}

	public String getUppercaseUnderscoreName() {
		if (name == null) return "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c) && i > 0 && Character.isLowerCase(name.charAt(i - 1))) {
				sb.append('_');
			}
			sb.append(Character.toUpperCase(c));
		}
		return sb.toString();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getColumnName() {
		return columnName != null ? columnName : name;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public String getExternalType() {
		return externalType;
	}

	public void setExternalType(String externalType) {
		this.externalType = externalType;
	}

	public String getValueClassName() {
		return valueClassName;
	}

	public String getFormatJavaType() {
		if (valueClassName == null) {
			return "Object";
		}
		switch (valueClassName) {
			case "NSString":
			case "java.lang.String":
				return "String";
			case "NSNumber":
				if ("i".equals(valueType)) return "Integer";
				if ("l".equals(valueType)) return "Long";
				if ("f".equals(valueType)) return "Float";
				if ("d".equals(valueType)) return "Double";
				if ("b".equals(valueType)) return "Boolean";
				if ("s".equals(valueType)) return "Short";
				if ("B".equals(valueType)) return "java.math.BigDecimal";
				return "Number";
			case "NSDecimalNumber":
				return "java.math.BigDecimal";
			case "NSCalendarDate":
				return "NSTimestamp";
			case "NSData":
				return "NSData";
			case "NSDictionary":
				return "NSDictionary";
			case "NSArray":
				return "NSArray";
			default:
				int lastDot = valueClassName.lastIndexOf('.');
				return lastDot > 0 ? valueClassName.substring(lastDot + 1) : valueClassName;
		}
	}

	public void setValueClassName(String valueClassName) {
		this.valueClassName = valueClassName;
	}

	public String getValueType() {
		return valueType;
	}

	public void setValueType(String valueType) {
		this.valueType = valueType;
	}

	public String getPrototypeName() {
		return prototypeName;
	}

	public void setPrototypeName(String prototypeName) {
		this.prototypeName = prototypeName;
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	public boolean isAllowsNull() {
		return allowsNull;
	}

	public void setAllowsNull(boolean allowsNull) {
		this.allowsNull = allowsNull;
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getPrecision() {
		return precision;
	}

	public void setPrecision(Integer precision) {
		this.precision = precision;
	}

	public Integer getScale() {
		return scale;
	}

	public void setScale(Integer scale) {
		this.scale = scale;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(boolean primaryKey) {
		this.primaryKey = primaryKey;
	}

	public boolean isClassProperty() {
		return classProperty;
	}

	public void setClassProperty(boolean classProperty) {
		this.classProperty = classProperty;
	}

	public boolean isUsedForLocking() {
		return usedForLocking;
	}

	public void setUsedForLocking(boolean usedForLocking) {
		this.usedForLocking = usedForLocking;
	}

	public boolean isClientClassProperty() {
		return clientClassProperty;
	}

	public void setClientClassProperty(boolean clientClassProperty) {
		this.clientClassProperty = clientClassProperty;
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

	public boolean isFlattened() {
		return definition != null && !definition.trim().isEmpty();
	}

	public void loadFromMap(Map<?, ?> map) {
		rawProperties.clear();
		if (map == null) return;
		rawProperties.putAll(map);
		EOModelMap m = new EOModelMap(map);

		this.name = m.getString("name", true);
		this.columnName = m.getString("columnName", true);
		this.externalType = m.getString("externalType", true);
		this.valueClassName = m.getString("valueClassName", true);
		this.valueType = m.getString("valueType", true);
		this.prototypeName = m.getString("prototypeName", true);
		this.definition = m.getString("definition", true);

		this.allowsNull = m.getBoolean("allowsNull", false);
		this.readOnly = m.getBoolean("readOnly", false);

		this.width = m.getInteger("width");
		this.precision = m.getInteger("precision");
		this.scale = m.getInteger("scale");

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
	}

	public EOModelMap toMap() {
		EOModelMap map = new EOModelMap(new PropertyListMap<>(rawProperties));
		map.setString("name", name, true);
		if (columnName != null && !columnName.trim().isEmpty()) {
			map.setString("columnName", columnName);
		} else {
			map.remove("columnName");
		}
		map.setString("externalType", externalType, true);
		map.setString("valueClassName", valueClassName, true);
		map.setString("valueType", valueType, true);
		map.setString("prototypeName", prototypeName, true);
		map.setString("definition", definition, true);

		map.setBoolean("allowsNull", allowsNull, EOModelMap.BooleanStyle.Y_N_OPTIONAL_DEFAULT_NO);
		map.setBoolean("readOnly", readOnly, EOModelMap.BooleanStyle.Y_N_OPTIONAL_DEFAULT_NO);

		map.setInteger("width", width);
		map.setInteger("precision", precision);
		map.setInteger("scale", scale);

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EOAttribute that = (EOAttribute) o;
		return Objects.equals(name, that.name);
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

