package org.wocommunity.plugins.intellij.eomodel.core;

import org.junit.jupiter.api.Test;
import org.wocommunity.plugins.intellij.eomodel.core.plist.PropertyListParserException;
import org.wocommunity.plugins.intellij.eomodel.core.plist.SimpleParserDataStructureFactory;
import org.wocommunity.plugins.intellij.eomodel.core.plist.WOLPropertyListSerialization;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WOLPropertyListSerializationTest {

	@Test
	public void testParseSimpleAsciiDictionary() throws PropertyListParserException {
		String plistStr = "{\n" +
				"    EOModelVersion = 2.1;\n" +
				"    adaptorName = JDBC;\n" +
				"    entities = (\n" +
				"        { name = User; className = \"com.example.User\"; }\n" +
				"    );\n" +
				"}";

		Object result = WOLPropertyListSerialization.propertyListFromString(plistStr, new SimpleParserDataStructureFactory());
		assertInstanceOf(Map.class, result);
		Map<?, ?> map = (Map<?, ?>) result;

		assertEquals("2.1", map.get("EOModelVersion").toString());
		assertEquals("JDBC", map.get("adaptorName"));

		Object entitiesObj = map.get("entities");
		assertInstanceOf(List.class, entitiesObj);
		List<?> entities = (List<?>) entitiesObj;
		assertEquals(1, entities.size());

		Map<?, ?> userEntity = (Map<?, ?>) entities.get(0);
		assertEquals("User", userEntity.get("name"));
		assertEquals("com.example.User", userEntity.get("className"));
	}

	@Test
	public void testSerializeAndDeserializeCycle() throws PropertyListParserException {
		String original = "{\n" +
				"    attributes = (\n" +
				"        { columnName = ID; name = id; valueClassName = NSNumber; },\n" +
				"        { columnName = NAME; name = name; valueClassName = NSString; width = 50; }\n" +
				"    );\n" +
				"    externalName = T_USER;\n" +
				"    name = User;\n" +
				"}";

		Object parsed = WOLPropertyListSerialization.propertyListFromString(original, new SimpleParserDataStructureFactory());
		String serialized = WOLPropertyListSerialization.stringFromPropertyList(parsed);
		assertNotNull(serialized);

		Object reParsed = WOLPropertyListSerialization.propertyListFromString(serialized, new SimpleParserDataStructureFactory());
		assertEquals(parsed, reParsed);
	}
}

