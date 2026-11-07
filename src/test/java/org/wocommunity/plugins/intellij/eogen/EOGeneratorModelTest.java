package org.wocommunity.plugins.intellij.eogen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.wocommunity.plugins.intellij.eogen.core.EODefine;
import org.wocommunity.plugins.intellij.eogen.core.EOGeneratorModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class EOGeneratorModelTest {

	@Test
	public void testParseString() throws Exception {
		String content = "-destination Sources\n" +
				"-extension java\n" +
				"-java\n" +
				"-model MyModel.eomodeld\n" +
				"-refmodel RefModel.eomodeld\n" +
				"-packagedirs\n" +
				"-subclassDestination Sources\n" +
				"-subclassJavaTemplate _Entity.java\n" +
				"-templatedir Templates\n" +
				"-verbose\n" +
				"-define-wonder 1\n" +
				"-define-customParam testValue";

		EOGeneratorModel model = new EOGeneratorModel();
		model.readFromString(content);

		assertEquals("Sources", model.getDestination());
		assertEquals("java", model.getExtension());
		assertTrue(model.isJava());
		assertEquals(Collections.singletonList("MyModel.eomodeld"), model.getModels());
		assertEquals(Collections.singletonList("RefModel.eomodeld"), model.getRefModels());
		assertTrue(model.isPackageDirs());
		assertEquals("Sources", model.getSubclassDestination());
		assertEquals("_Entity.java", model.getSubclassJavaTemplate());
		assertEquals("Templates", model.getTemplateDir());
		assertTrue(model.isVerbose());

		assertEquals(2, model.getDefines().size());
		assertEquals("wonder", model.getDefines().get(0).getName());
		assertEquals("1", model.getDefines().get(0).getValue());
		assertEquals("customParam", model.getDefines().get(1).getName());
		assertEquals("testValue", model.getDefines().get(1).getValue());
	}

	@Test
	public void testSerializeToString() {
		EOGeneratorModel model = new EOGeneratorModel();
		model.setDestination("Sources/main/java");
		model.setSubclassDestination("Sources/main/java");
		model.setJava(true);
		model.setPackageDirs(true);
		model.setModels(Arrays.asList("ModelA.eomodeld", "ModelB.eomodeld"));
		model.setDefines(Collections.singletonList(new EODefine("foo", "bar")));

		String serialized = model.writeToString();
		assertTrue(serialized.contains("-destination Sources/main/java"));
		assertTrue(serialized.contains("-subclassDestination Sources/main/java"));
		assertTrue(serialized.contains("-model ModelA.eomodeld"));
		assertTrue(serialized.contains("-model ModelB.eomodeld"));
		assertTrue(serialized.contains("-define-foo bar"));
		assertTrue(serialized.contains("-java"));
		assertTrue(serialized.contains("-packagedirs"));
	}

	@Test
	public void testQuotedArgumentsSerialization() throws Exception {
		EOGeneratorModel model = new EOGeneratorModel();
		model.setDestination("Folder With Spaces/Sources");
		String serialized = model.writeToString();
		assertTrue(serialized.contains("-destination \"Folder With Spaces/Sources\""));

		EOGeneratorModel reloaded = new EOGeneratorModel();
		reloaded.readFromString(serialized);
		assertEquals("Folder With Spaces/Sources", reloaded.getDestination());
	}

	@Test
	public void testReadAndWriteFile(@TempDir Path tempDir) throws Exception {
		File file = tempDir.resolve("test.eogen").toFile();

		EOGeneratorModel model = new EOGeneratorModel();
		model.setDestination("src/main/java");
		model.setSubclassDestination("src/main/java");
		model.setModels(Collections.singletonList("TestModel.eomodeld"));
		model.setJava(true);
		model.setPackageDirs(true);
		model.setVerbose(true);
		model.setDefines(Collections.singletonList(new EODefine("wonder", "1")));

		model.writeToFile(file);
		assertTrue(file.exists());

		EOGeneratorModel loaded = new EOGeneratorModel();
		loaded.readFromFile(file);

		assertEquals("src/main/java", loaded.getDestination());
		assertEquals("src/main/java", loaded.getSubclassDestination());
		assertEquals(Collections.singletonList("TestModel.eomodeld"), loaded.getModels());
		assertTrue(loaded.isJava());
		assertTrue(loaded.isPackageDirs());
		assertTrue(loaded.isVerbose());
		assertEquals(1, loaded.getDefines().size());
		assertEquals("wonder", loaded.getDefines().get(0).getName());
		assertEquals("1", loaded.getDefines().get(0).getValue());
	}
}
