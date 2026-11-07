package org.wocommunity.plugins.intellij.eogen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.wocommunity.plugins.intellij.eogen.core.EOGeneratorModel;
import org.wocommunity.plugins.intellij.eogen.core.EOGeneratorRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

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

		EOGeneratorRunner.GenerationResult result = EOGeneratorRunner.generate(null, eogenFile, model, null);
		assertTrue(result.isSuccess(), "Generation should succeed: " + result.getMessages());
		assertFalse(result.getGeneratedFiles().isEmpty(), "Should have generated files");

		File artistSuperclass = tempDir.resolve("src/main/java/org/objectstyle/art/_Artist.java").toFile();
		File artistSubclass = tempDir.resolve("src/main/java/org/objectstyle/art/Artist.java").toFile();

		assertTrue(artistSuperclass.exists(), "_Artist.java superclass should exist");
		assertTrue(artistSubclass.exists(), "Artist.java subclass should exist");

		String superclassContent = Files.readString(artistSuperclass.toPath());
		assertTrue(superclassContent.contains("package org.objectstyle.art;"), "Should declare correct package");
		assertTrue(superclassContent.contains("public abstract class _Artist"), "Should declare abstract superclass");
		assertTrue(superclassContent.contains("public static final String ARTISTNAME_KEY = \"artistName\";"), "Should define attribute keys");
		assertTrue(superclassContent.contains("public String artistName()"), "Should have artistName getter");
		assertTrue(superclassContent.contains("public void setArtistName(String value)"), "Should have artistName setter");
		assertTrue(superclassContent.contains("public NSArray<Painting> paintingArray()"), "Should have relationship getter");

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
}
