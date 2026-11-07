package org.wocommunity.plugins.intellij.eomodel.core.plist;

import java.util.Collection;
import java.util.TreeSet;

public class PropertyListSet<T> extends TreeSet<T> {
	public PropertyListSet() {
		super(PropertyListComparator.AscendingInsensitivePropertyListComparator);
	}

	public PropertyListSet(Object[] guideArray) {
		super(PropertyListComparator.propertyListComparatorWithGuideArray(guideArray));
	}

	public PropertyListSet(Collection<T> set) {
		this();
		if (set != null) {
			addAll(set);
		}
	}
}

