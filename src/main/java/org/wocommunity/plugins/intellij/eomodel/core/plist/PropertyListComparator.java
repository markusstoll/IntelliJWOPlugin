package org.wocommunity.plugins.intellij.eomodel.core.plist;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PropertyListComparator implements Comparator<Object> {

	public static final PropertyListComparator AscendingInsensitivePropertyListComparator = new PropertyListComparator(true);
	public static final PropertyListComparator AscendingSensitivePropertyListComparator = new PropertyListComparator(false);

	private final boolean caseInsensitive;
	protected Map<Object, Integer> guideMap;

	public static PropertyListComparator propertyListComparatorWithGuideArray(Object[] guideArray) {
		if (guideArray != null && guideArray.length > 0) {
			Object[] sortedArray = guideArray.clone();
			Arrays.sort(sortedArray, AscendingInsensitivePropertyListComparator);

			if (!Arrays.equals(guideArray, sortedArray)) {
				PropertyListComparator result = new PropertyListComparator(true);
				int size = guideArray.length;
				if (size > 0) {
					result.guideMap = new HashMap<>(size);

					for (int i = 0; i < size; ++i) {
						Object origEntry = guideArray[i];
						Object oneEntry = origEntry;
						if (origEntry instanceof Map) {
							Object name = ((Map<?, ?>) origEntry).get("name");
							if (name == null) {
								name = ((Map<?, ?>) origEntry).get("prototypeName");
							}
							if (name != null) {
								oneEntry = name;
							}
						}
						if (oneEntry != null) {
							result.guideMap.put(oneEntry, i);
						}
					}
				}
				return result;
			}
		}

		return AscendingInsensitivePropertyListComparator;
	}

	public PropertyListComparator(boolean caseInsensitive) {
		this.caseInsensitive = caseInsensitive;
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public int compare(Object arg0, Object arg1) {
		if (arg0 == null) {
			return (arg1 == null) ? 0 : -1;
		} else if (arg1 == null) {
			return 1;
		} else if (guideMap != null && (guideMap.get(arg0) != null || guideMap.get(arg1) != null)) {
			Integer guide0 = guideMap.get(arg0);
			Integer guide1 = guideMap.get(arg1);

			if (guide0 == null) {
				return 1;
			} else if (guide1 == null) {
				return -1;
			} else {
				return guide0.compareTo(guide1);
			}
		} else if (arg0 instanceof String && arg1 instanceof String) {
			if (caseInsensitive) {
				return ((String) arg0).compareToIgnoreCase((String) arg1);
			}
			return ((String) arg0).compareTo((String) arg1);
		} else if (arg0 instanceof Number && arg1 instanceof Number) {
			double d0 = ((Number) arg0).doubleValue();
			double d1 = ((Number) arg1).doubleValue();
			return Double.compare(d0, d1);
		} else if (arg0 instanceof Timestamp && arg1 instanceof Timestamp) {
			return ((Timestamp) arg0).compareTo((Timestamp) arg1);
		} else if (arg0 instanceof Map && arg1 instanceof Map) {
			Map dic0 = (Map) arg0;
			Map dic1 = (Map) arg1;
			Object key0 = dic0.get("name");
			if (key0 == null) {
				key0 = dic0.get("prototypeName");
			}
			Object key1 = dic1.get("name");
			if (key1 == null) {
				key1 = dic1.get("prototypeName");
			}
			if (key0 != null && key1 != null) {
				return compare(key0, key1);
			} else if (key0 != null || key1 != null) {
				return key0 != null ? -1 : 1;
			}
			Set allKeys0 = dic0.keySet();
			Set allKeys1 = dic1.keySet();
			Iterator allKeys0Iter = allKeys0.iterator();
			Iterator allKeys1Iter = allKeys1.iterator();
			while (allKeys0Iter.hasNext() && allKeys1Iter.hasNext()) {
				key0 = allKeys0Iter.next();
				key1 = allKeys1Iter.next();
				int compareResult = compare(key0, key1);
				if (compareResult != 0) {
					return compareResult;
				}
				compareResult = compare(dic0.get(key0), dic1.get(key1));
				if (compareResult != 0) {
					return compareResult;
				}
			}
			return Integer.compare(allKeys0.size(), allKeys1.size());
		} else {
			int compareResult = compare(arg0.getClass().getName(), arg1.getClass().getName());
			if (compareResult != 0) {
				return compareResult;
			}
			return arg0.toString().compareTo(arg1.toString());
		}
	}
}

