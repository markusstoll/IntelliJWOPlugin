package org.wocommunity.plugins.intellij.eomodel.core.plist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SimpleParserDataStructureFactory implements ParserDataStructureFactory {
	@Override
	public Collection<Object> createCollection(String keyPath) {
		return new ArrayList<>();
	}

	@Override
	public Map<Object, Object> createMap(String keyPath) {
		return new HashMap<>();
	}
}

