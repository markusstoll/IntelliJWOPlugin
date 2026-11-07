package org.wocommunity.plugins.intellij.eomodel.core.plist;

import java.util.Collection;
import java.util.Map;

public interface ParserDataStructureFactory {
	Map<Object, Object> createMap(String keyPath);

	Collection<Object> createCollection(String keyPath);
}

