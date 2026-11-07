package org.wocommunity.plugins.intellij.eomodel.core.plist;

import java.util.Map;
import java.util.TreeMap;

public class PropertyListMap<K, V> extends TreeMap<K, V> {
	public PropertyListMap() {
		super(PropertyListComparator.AscendingSensitivePropertyListComparator);
	}

	public PropertyListMap(Map<K, V> map) {
		this();
		if (map != null) {
			putAll(map);
		}
	}
}

