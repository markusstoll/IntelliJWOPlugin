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
}
