package org.wocommunity.plugins.intellij.eomodel.core.model;

import org.wocommunity.plugins.intellij.eomodel.core.plist.PropertyListMap;
import org.wocommunity.plugins.intellij.eomodel.core.plist.PropertyListSet;

import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EOModelMap implements Map<Object, Object>, Cloneable {

	public enum BooleanStyle {
		YES_NO,
		Y_N,
		Y_N_OPTIONAL_DEFAULT_NO,
		Y_N_OPTIONAL_DEFAULT_YES
	}

	private final Map<Object, Object> backingMap;

	public EOModelMap() {
		this(new PropertyListMap<>());
	}

	public EOModelMap(Map backingMap) {
		if (backingMap == null) {
			this.backingMap = new PropertyListMap<>();
		} else {
			this.backingMap = backingMap;
		}
	}

	public Map<Object, Object> getBackingMap() {
		return backingMap;
	}

	public String getString(String key) {
		return getString(key, false);
	}

	public String getString(String key, boolean emptyIsNull) {
		Object value = backingMap.get(key);
		if (value == null) {
			return null;
		}
		String str = value.toString();
		if (emptyIsNull && str.trim().isEmpty()) {
			return null;
		}
		return str;
	}

	public void setString(String key, String value) {
		setString(key, value, false);
	}

	public void setString(String key, String value, boolean removeIfNullOrEmpty) {
		if (value == null || (removeIfNullOrEmpty && value.trim().isEmpty())) {
			backingMap.remove(key);
		} else {
			backingMap.put(key, value);
		}
	}

	public Integer getInteger(String key) {
		Object value = backingMap.get(key);
		if (value == null) {
			return null;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		try {
			return Integer.parseInt(value.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public int getInteger(String key, int defaultValue) {
		Integer val = getInteger(key);
		return val != null ? val : defaultValue;
	}

	public void setInteger(String key, Integer value) {
		if (value == null) {
			backingMap.remove(key);
		} else {
			backingMap.put(key, value);
		}
	}

	public Boolean getBoolean(String key) {
		Object value = backingMap.get(key);
		if (value == null) {
			return null;
		}
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		String str = value.toString().trim();
		if ("Y".equalsIgnoreCase(str) || "YES".equalsIgnoreCase(str) || "TRUE".equalsIgnoreCase(str)) {
			return Boolean.TRUE;
		}
		if ("N".equalsIgnoreCase(str) || "NO".equalsIgnoreCase(str) || "FALSE".equalsIgnoreCase(str)) {
			return Boolean.FALSE;
		}
		return null;
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		Boolean val = getBoolean(key);
		return val != null ? val : defaultValue;
	}

	public void setBoolean(String key, Boolean value, BooleanStyle style) {
		if (value == null) {
			backingMap.remove(key);
			return;
		}
		switch (style) {
			case YES_NO:
				backingMap.put(key, value ? "YES" : "NO");
				break;
			case Y_N_OPTIONAL_DEFAULT_NO:
				if (value) {
					backingMap.put(key, "Y");
				} else {
					backingMap.remove(key);
				}
				break;
			case Y_N_OPTIONAL_DEFAULT_YES:
				if (!value) {
					backingMap.put(key, "N");
				} else {
					backingMap.remove(key);
				}
				break;
			case Y_N:
			default:
				backingMap.put(key, value ? "Y" : "N");
				break;
		}
	}

	public Map<Object, Object> getMap(String key) {
		Object value = backingMap.get(key);
		if (value instanceof Map) {
			return (Map<Object, Object>) value;
		}
		return null;
	}

	public void setMap(String key, Map map) {
		if (map == null || map.isEmpty()) {
			backingMap.remove(key);
		} else {
			backingMap.put(key, map);
		}
	}

	public List<Object> getList(String key) {
		Object value = backingMap.get(key);
		if (value instanceof List) {
			return (List<Object>) value;
		} else if (value instanceof Collection) {
			return new ArrayList<>((Collection<?>) value);
		}
		return null;
	}

	public void setList(String key, List list) {
		if (list == null || list.isEmpty()) {
			backingMap.remove(key);
		} else {
			backingMap.put(key, list);
		}
	}

	public Set<Object> getSet(String key) {
		Object value = backingMap.get(key);
		if (value instanceof Set) {
			return (Set<Object>) value;
		} else if (value instanceof Collection) {
			return new PropertyListSet<>((Collection) value);
		}
		return null;
	}

	public void setSet(String key, Set set) {
		if (set == null || set.isEmpty()) {
			backingMap.remove(key);
		} else {
			backingMap.put(key, set);
		}
	}

	// Map interface delegations
	@Override
	public int size() {
		return backingMap.size();
	}

	@Override
	public boolean isEmpty() {
		return backingMap.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return backingMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return backingMap.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return backingMap.get(key);
	}

	@Override
	public Object put(Object key, Object value) {
		return backingMap.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return backingMap.remove(key);
	}

	@Override
	public void putAll(Map<?, ?> m) {
		backingMap.putAll(m);
	}

	@Override
	public void clear() {
		backingMap.clear();
	}

	@Override
	public Set<Object> keySet() {
		return backingMap.keySet();
	}

	@Override
	public Collection<Object> values() {
		return backingMap.values();
	}

	@Override
	public Set<Entry<Object, Object>> entrySet() {
		return backingMap.entrySet();
	}

	@Override
	public EOModelMap clone() {
		PropertyListMap<Object, Object> copy = new PropertyListMap<>();
		copy.putAll(backingMap);
		return new EOModelMap(copy);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EOModelMap that = (EOModelMap) o;
		return Objects.equals(backingMap, that.backingMap);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(backingMap);
	}

	@Override
	public String toString() {
		return backingMap.toString();
	}
}

