package org.wocommunity.plugins.intellij.eogen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.wocommunity.plugins.intellij.eogen.core.EOGeneratorModel;
import org.wocommunity.plugins.intellij.eogen.core.EOGeneratorRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EOGeneratorRunnerTest {

	@Test
	public void testGenerationFromArtModel(@TempDir Path tempDir) throws IOException {
		File modelDir = new File("src/test/resources/eomodeld/art.eomodeld");
		assertTrue(modelDir.exists(), "art.eomodeld must exist in test resources");

		File eogenFile = tempDir.resolve("test.eogen").toFile();

		EOGeneratorModel model = new EOGeneratorModel();
		model.setDestination("src/main/java");
		model.setSubclassDestination("src/main/java");
		model.setModels(Collections.singletonList(modelDir.getAbsolutePath()));
		model.setJava(true);
		model.setPackageDirs(true);

		List<String> streamedLogs = new ArrayList<>();
		EOGeneratorRunner.GenerationResult result = EOGeneratorRunner.generate(null, eogenFile, model, null, streamedLogs::add);
		assertTrue(result.isSuccess(), "Generation should succeed: " + result.getMessages());
		assertFalse(result.getGeneratedFiles().isEmpty(), "Should have generated files");
		assertFalse(streamedLogs.isEmpty(), "Should have received streamed logs");

		File artistSuperclass = tempDir.resolve("src/main/java/org/objectstyle/art/_Artist.java").toFile();
		File artistSubclass = tempDir.resolve("src/main/java/org/objectstyle/art/Artist.java").toFile();

		assertTrue(artistSuperclass.exists(), "_Artist.java superclass should exist");
		assertTrue(artistSubclass.exists(), "Artist.java subclass should exist");

		String superclassContent = Files.readString(artistSuperclass.toPath());
		assertTrue(superclassContent.contains("package org.objectstyle.art;"), "Should declare correct package");
		assertTrue(superclassContent.contains("public abstract class _Artist"), "Should declare abstract superclass");
		assertTrue(superclassContent.contains("public static final String ARTIST_NAME_KEY = \"artistName\";"), "Should define attribute keys");
		assertTrue(superclassContent.contains("public String artistName()"), "Should have artistName getter");
		assertTrue(superclassContent.contains("public void setArtistName(String value)"), "Should have artistName setter");
		assertTrue(superclassContent.contains("paintingArray()"), "Should have relationship getter");
		assertFalse(superclassContent.contains("artistId()"), "artistId is not a class property and must not have a getter");
		assertFalse(superclassContent.contains("ARTIST_ID_KEY"), "artistId is not a class property and must not have a key constant");
		assertTrue(superclassContent.contains("dateOfBirth()"), "dateOfBirth is a class property and must have a getter");

		String subclassContent = Files.readString(artistSubclass.toPath());
		assertTrue(subclassContent.contains("public class Artist extends _Artist"), "Subclass should extend _Artist");

		// Test non-overwrite of subclass on second run
		String modifiedSubclass = subclassContent + "\n// Custom logic\n";
		Files.writeString(artistSubclass.toPath(), modifiedSubclass);

		EOGeneratorRunner.GenerationResult result2 = EOGeneratorRunner.generate(null, eogenFile, model, null);
		assertTrue(result2.isSuccess());

		String reloadedSubclass = Files.readString(artistSubclass.toPath());
		assertTrue(reloadedSubclass.contains("// Custom logic"), "Existing subclass should NOT be overwritten");
	}

	@Test
	public void testInferredModelFromEogenName(@TempDir Path tempDir) throws IOException {
		// Create art.eomodeld in tempDir
		File artSource = new File("src/test/resources/eomodeld/art.eomodeld");
		Path artTarget = tempDir.resolve("art.eomodeld");
		Files.createDirectories(artTarget);
		for (File f : artSource.listFiles()) {
			if (f.isFile()) {
				Files.copy(f.toPath(), artTarget.resolve(f.getName()));
			}
		}

		// .eogen file named art.eogen with NO -model argument
		File eogenFile = tempDir.resolve("art.eogen").toFile();

		EOGeneratorModel model = new EOGeneratorModel();
		model.setDestination("src/main/java");
		model.setSubclassDestination("src/main/java");
		model.setJava(true);

		EOGeneratorRunner.GenerationResult result = EOGeneratorRunner.generate(null, eogenFile, model, null);
		assertTrue(result.isSuccess(), "Should succeed by inferring art.eomodeld");
		assertFalse(result.getGeneratedFiles().isEmpty());
	}

	@Test
	public void testMissingModelGeneratesZeroFilesAndLogsDiagnostics(@TempDir Path tempDir) {
		File eogenFile = tempDir.resolve("nonexistent.eogen").toFile();

		EOGeneratorModel model = new EOGeneratorModel();
		model.setDestination("src/main/java");
		model.setModels(Collections.singletonList("DoesNotExist.eomodeld"));

		EOGeneratorRunner.GenerationResult result = EOGeneratorRunner.generate(null, eogenFile, model, null);
		assertFalse(result.isSuccess(), "Should fail when 0 files generated");
		assertTrue(result.getGeneratedFiles().isEmpty());

		boolean hasDiagnostic = result.getMessages().stream().anyMatch(m -> m.contains("Model folder not found"));
		assertTrue(hasDiagnostic, "Log must contain diagnostic about missing model folder");
	}

	@Test
	public void testSuperclassPackageGeneration(@TempDir Path tempDir) throws IOException {
		File modelDir = new File("src/test/resources/eomodeld/art.eomodeld");
		File eogenFile = tempDir.resolve("test.eogen").toFile();

		EOGeneratorModel model = new EOGeneratorModel();
		model.setDestination("src/main/java");
		model.setSubclassDestination("src/main/java");
		model.setModels(Collections.singletonList(modelDir.getAbsolutePath()));
		model.setJava(true);
		model.setPackageDirs(true);
		model.setSuperclassPackage("_");

		EOGeneratorRunner.GenerationResult result = EOGeneratorRunner.generate(null, eogenFile, model, null);
		assertTrue(result.isSuccess(), "Generation should succeed: " + result.getMessages());

		File superclassFile = tempDir.resolve("src/main/java/org/objectstyle/art/_/_Artist.java").toFile();
		File subclassFile = tempDir.resolve("src/main/java/org/objectstyle/art/Artist.java").toFile();

		assertTrue(superclassFile.exists(), "_Artist.java should be in _ subfolder: " + superclassFile.getAbsolutePath());
		assertTrue(subclassFile.exists(), "Artist.java should be in org.objectstyle.art");

		String superclassContent = Files.readString(superclassFile.toPath());
		assertTrue(superclassContent.contains("package org.objectstyle.art._;"), "Superclass should declare subpackage: " + superclassContent);

		String subclassContent = Files.readString(subclassFile.toPath());
		assertTrue(subclassContent.contains("package org.objectstyle.art;"), "Subclass should declare base package");
		assertTrue(subclassContent.contains("import org.objectstyle.art._._Artist;") || subclassContent.contains("extends org.objectstyle.art._._Artist"),
				"Subclass should reference superclass in subpackage");
	}

	@Test
	public void testWonderProjectUsesWonderEntityTemplate(@TempDir Path tempDir) throws IOException {
		File modelDir = new File("src/test/resources/eomodeld/art.eomodeld");
		File eogenFile = tempDir.resolve("test.eogen").toFile();

		EOGeneratorModel model = new EOGeneratorModel();
		model.setDestination("src/main/java");
		model.setSubclassDestination("src/main/java");
		model.setModels(Collections.singletonList(modelDir.getAbsolutePath()));
		model.setJava(true);
		model.setPackageDirs(true);
		model.addDefine("wonder", "1");

		List<String> logs = new ArrayList<>();
		EOGeneratorRunner.GenerationResult result = EOGeneratorRunner.generate(null, eogenFile, model, null, logs::add);
		assertTrue(result.isSuccess(), "Generation should succeed: " + result.getMessages());

		File artistSuperclass = tempDir.resolve("src/main/java/org/objectstyle/art/_Artist.java").toFile();
		assertTrue(artistSuperclass.exists());

		String content = Files.readString(artistSuperclass.toPath());
		assertTrue(content.contains("ERXGenericRecord"), "Wonder template should extend ERXGenericRecord: " + content);
		assertTrue(content.contains("ERXKey"), "Wonder template should define ERXKey fields: " + content);
		assertFalse(content.contains("ARTIST_ID"), "artistId is not a class property and must not have an ERXKey: " + content);
		assertTrue(content.contains("ARTIST_NAME"), "artistName is a class property and must have an ERXKey: " + content);
	}

	@Test
	public void testCustomValueClassNamePreservesPackage() {
		org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute attr =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute("priority");
		attr.setValueClassName("de.junidas.pe4.data.SortPriority");
		assertEquals("de.junidas.pe4.data.SortPriority", attr.getFormatJavaType());
		assertEquals("de.junidas.pe4.data.SortPriority", attr.getJavaClassName());

		org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute stringAttr =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute("name");
		stringAttr.setValueClassName("java.lang.String");
		assertEquals("String", stringAttr.getFormatJavaType());

		org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute intAttr =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute("count");
		intAttr.setValueClassName("NSNumber");
		intAttr.setValueType("i");
		assertEquals("Integer", intAttr.getFormatJavaType());

		org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute bigDecimalAttr =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute("amount");
		bigDecimalAttr.setValueClassName("NSDecimalNumber");
		assertEquals("java.math.BigDecimal", bigDecimalAttr.getFormatJavaType());

		org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute boolAttr =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute("isEdited");
		boolAttr.setValueClassName("NSNumber");
		boolAttr.setValueType("c");
		assertEquals("Boolean", boolAttr.getFormatJavaType());
		assertEquals("Boolean", boolAttr.getJavaClassName());

		org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute byteAttr =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute("flagByte");
		byteAttr.setValueClassName("NSNumber");
		byteAttr.setValueType("b");
		assertEquals("Byte", byteAttr.getFormatJavaType());
	}

	@Test
	public void testUppercaseUnderscoreName() {
		org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute attr =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute("fkLandingPageMPElementId");
		assertEquals("FK_LANDING_PAGE_MP_ELEMENT_ID", attr.getUppercaseUnderscoreName());

		org.wocommunity.plugins.intellij.eomodel.core.model.EORelationship rel =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EORelationship("fkLandingPageMPElementId");
		assertEquals("FK_LANDING_PAGE_MP_ELEMENT_ID", rel.getUppercaseUnderscoreName());

		org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute idAttr =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute("id");
		assertEquals("ID", idAttr.getUppercaseUnderscoreName());

		org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute artistIdAttr =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute("artistId");
		assertEquals("ARTIST_ID", artistIdAttr.getUppercaseUnderscoreName());
	}

	@Test
	public void testInverseRelationshipResolution() {
		org.wocommunity.plugins.intellij.eomodel.core.model.EOModel model =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EOModel();

		// Self-referencing entity: Element (toParent <-> children)
		org.wocommunity.plugins.intellij.eomodel.core.model.EOEntity elementEntity =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EOEntity("Element");
		model.addEntity(elementEntity);

		org.wocommunity.plugins.intellij.eomodel.core.model.EORelationship toParent =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EORelationship("toParent");
		toParent.setDestinationEntityName("Element");
		toParent.setToMany(false);
		toParent.addJoin(new org.wocommunity.plugins.intellij.eomodel.core.model.EOJoin("parentId", "id"));
		elementEntity.addRelationship(toParent);

		org.wocommunity.plugins.intellij.eomodel.core.model.EORelationship children =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EORelationship("children");
		children.setDestinationEntityName("Element");
		children.setToMany(true);
		children.addJoin(new org.wocommunity.plugins.intellij.eomodel.core.model.EOJoin("id", "parentId"));
		elementEntity.addRelationship(children);

		// Must NOT return itself (the forward relationship)!
		assertNotEquals(toParent, toParent.getInverseRelationship(), "Inverse of toParent must not be toParent itself");
		assertEquals(children, toParent.getInverseRelationship());
		assertNotEquals(children, children.getInverseRelationship(), "Inverse of children must not be children itself");
		assertEquals(toParent, children.getInverseRelationship());
	}

	@Test
	public void testSpaceGobblingBcModeDoesNotGenerateBlankLinesForNullableAttributes() {
		org.wocommunity.plugins.intellij.eomodel.core.model.EOEntity entity =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EOEntity("Test");
		entity.setClassName("com.test.Test");

		org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute a1 =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute("attr1Null");
		a1.setAllowsNull(true);
		a1.setClassProperty(true);
		entity.addAttribute(a1);

		org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute a2 =
				new org.wocommunity.plugins.intellij.eomodel.core.model.EOAttribute("attr2NotNull");
		a2.setAllowsNull(false);
		a2.setClassProperty(true);
		entity.addAttribute(a2);

		String templateWithForeachOnSameLine =
				"public static Foo createFoo(EOEditingContext editingContext#foreach ($attribute in $entity.sortedClassAttributes)\n" +
				"#if (!$attribute.allowsNull)\n" +
				", ${attribute.javaClassName} ${attribute.name}\n" +
				"#end\n" +
				"#end\n" +
				")";

		org.apache.velocity.app.VelocityEngine ve = new org.apache.velocity.app.VelocityEngine();
		ve.setProperty("parser.space_gobbling", "bc");
		ve.init();

		org.apache.velocity.VelocityContext ctx = new org.apache.velocity.VelocityContext();
		ctx.put("entity", entity);

		java.io.StringWriter sw = new java.io.StringWriter();
		ve.evaluate(ctx, sw, "test", templateWithForeachOnSameLine);
		String result = sw.toString();

		// In 'bc' (backward compatible) mode, the skipped nullable attribute must NOT introduce an empty line in the param list
		assertFalse(result.contains("\n\n"), "Output should not contain empty lines: " + result);
		assertTrue(result.contains("createFoo(EOEditingContext editingContext, Object attr2NotNull"),
				"Should contain non-null attribute directly without empty line: " + result);
	}

	@Test
	public void testEntityModelerParentClassNameAndGenerateSource(@TempDir Path tempDir) throws IOException {
		File modelDir = tempDir.resolve("Test.eomodeld").toFile();
		assertTrue(modelDir.mkdirs());

		// index.eomodeld
		File indexFile = new File(modelDir, "index.eomodeld");
		Files.writeString(indexFile.toPath(),
				"{\n" +
				"    EOModelVersion = \"2.1\";\n" +
				"    entities = (\n" +
				"        {className = \"de.junidas.bosch.ptextranet.data.SubAuftrag\"; name = SubAuftrag; },\n" +
				"        {className = \"ERXGenericRecord\"; name = GenericEntity; },\n" +
				"        {className = \"de.junidas.bosch.ptextranet.data.SkippedEntity\"; name = SkippedEntity; }\n" +
				"    );\n" +
				"}\n");

		// SubAuftrag.plist with parentClassName
		File subAuftragFile = new File(modelDir, "SubAuftrag.plist");
		Files.writeString(subAuftragFile.toPath(),
				"{\n" +
				"    name = SubAuftrag;\n" +
				"    className = \"de.junidas.bosch.ptextranet.data.SubAuftrag\";\n" +
				"    userInfo = {\n" +
				"        \"_EntityModeler\" = {parentClassName = \"de.junidas.bosch.ptextranet.data.Auftrag\"; };\n" +
				"    };\n" +
				"    attributes = ();\n" +
				"}\n");

		// GenericEntity.plist with className = ERXGenericRecord
		File genericFile = new File(modelDir, "GenericEntity.plist");
		Files.writeString(genericFile.toPath(),
				"{\n" +
				"    name = GenericEntity;\n" +
				"    className = \"ERXGenericRecord\";\n" +
				"    attributes = ();\n" +
				"}\n");

		// SkippedEntity.plist with generateSource = NO
		File skippedFile = new File(modelDir, "SkippedEntity.plist");
		Files.writeString(skippedFile.toPath(),
				"{\n" +
				"    name = SkippedEntity;\n" +
				"    className = \"de.junidas.bosch.ptextranet.data.SkippedEntity\";\n" +
				"    userInfo = {\n" +
				"        \"_EntityModeler\" = {generateSource = NO; };\n" +
				"    };\n" +
				"    attributes = ();\n" +
				"}\n");

		File eogenFile = tempDir.resolve("test.eogen").toFile();

		EOGeneratorModel model = new EOGeneratorModel();
		model.setDestination("src/main/java");
		model.setSubclassDestination("src/main/java");
		model.setModels(Collections.singletonList(modelDir.getAbsolutePath()));
		model.setJava(true);
		model.setPackageDirs(true);

		List<String> logs = new ArrayList<>();
		EOGeneratorRunner.GenerationResult result = EOGeneratorRunner.generate(null, eogenFile, model, null, logs::add);
		assertTrue(result.isSuccess(), "Generation should succeed: " + result.getMessages());

		// SubAuftrag should be generated and extend parentClassName: de.junidas.bosch.ptextranet.data.Auftrag
		File superclassFile = tempDir.resolve("src/main/java/de/junidas/bosch/ptextranet/data/_SubAuftrag.java").toFile();
		assertTrue(superclassFile.exists(), "_SubAuftrag.java should be generated");
		String superclassContent = Files.readString(superclassFile.toPath());
		assertTrue(superclassContent.contains("public abstract class _SubAuftrag extends de.junidas.bosch.ptextranet.data.Auftrag"),
				"Should extend parentClassName from _EntityModeler, but was: " + superclassContent);

		// ERXGenericRecord entity should be skipped!
		File genericSuperclass = tempDir.resolve("src/main/java/_GenericEntity.java").toFile();
		assertFalse(genericSuperclass.exists(), "ERXGenericRecord entity should not have generated code");
		File genericDirect = tempDir.resolve("src/main/java/_ERXGenericRecord.java").toFile();
		assertFalse(genericDirect.exists(), "Must never generate _ERXGenericRecord.java");

		// SkippedEntity should be skipped!
		File skippedSuperclass = tempDir.resolve("src/main/java/de/junidas/bosch/ptextranet/data/_SkippedEntity.java").toFile();
		assertFalse(skippedSuperclass.exists(), "Entity with generateSource=NO must not have generated code");
	}
}
