package org.wocommunity.plugins.intellij.eomodel.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.wocommunity.plugins.intellij.eomodel.core.model.*;
import org.wocommunity.plugins.intellij.eomodel.core.plist.PropertyListParserException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EOModelCoreTest {

	@Test
	public void testLoadSampleArtModel() throws IOException, PropertyListParserException {
		File modelFolder = new File("src/test/resources/eomodeld/art.eomodeld");
		assertTrue(modelFolder.exists(), "Sample art.eomodeld should exist");

		EOModel model = new EOModel();
		model.loadFromFolder(modelFolder);

		assertEquals("art", model.getName());
		assertEquals("2.1", model.getVersion());
		assertEquals("None", model.getAdaptorName());

		List<EOEntity> entities = model.getEntities();
		assertEquals(6, entities.size());

		EOEntity artist = model.getEntity("Artist");
		assertNotNull(artist);
		assertEquals("Artist", artist.getName());
		assertEquals("ARTIST", artist.getExternalName());
		assertEquals("org.objectstyle.art.Artist", artist.getClassName());

		// Test attributes
		assertEquals(3, artist.getAttributes().size());
		EOAttribute artistId = artist.getAttribute("artistId");
		assertNotNull(artistId);
		assertEquals("ARTIST_ID", artistId.getColumnName());
		assertEquals("int", artistId.getExternalType());
		assertEquals("NSNumber", artistId.getValueClassName());
		assertEquals("i", artistId.getValueType());
		assertTrue(artistId.isPrimaryKey());
		assertTrue(artistId.isUsedForLocking());
		assertFalse(artistId.isAllowsNull());

		EOAttribute dateOfBirth = artist.getAttribute("dateOfBirth");
		assertNotNull(dateOfBirth);
		assertTrue(dateOfBirth.isAllowsNull());
		assertTrue(dateOfBirth.isClassProperty());

		// Test relationships
		assertEquals(3, artist.getRelationships().size());
		EORelationship paintingArray = artist.getRelationship("paintingArray");
		assertNotNull(paintingArray);
		assertTrue(paintingArray.isToMany());
		assertEquals("Painting", paintingArray.getDestinationEntityName());
		assertEquals(EODeleteRule.CASCADE, paintingArray.getDeleteRule());
		assertEquals(EOJoinSemantic.INNER, paintingArray.getJoinSemantic());

		// Test joins resolution
		assertEquals(1, paintingArray.getJoins().size());
		EOJoin join = paintingArray.getJoins().get(0);
		assertEquals("artistId", join.getSourceAttributeName());
		assertEquals("artistId", join.getDestinationAttributeName());
		assertNotNull(join.getSourceAttribute(), "Source attribute should be resolved");
		assertNotNull(join.getDestinationAttribute(), "Destination attribute should be resolved");
		assertSame(artistId, join.getSourceAttribute());
		assertEquals("Painting", join.getDestinationAttribute().getEntity().getName());
	}

	@Test
	public void testModifyAndSaveModelCycle(@TempDir Path tempDir) throws IOException, PropertyListParserException {
		File sourceFolder = new File("src/test/resources/eomodeld/art.eomodeld");
		EOModel model = new EOModel();
		model.loadFromFolder(sourceFolder);

		// 1. Modify model: change adaptor & connection dictionary
		model.setAdaptorName("JDBC");
		model.getConnectionDictionary().setUrl("jdbc:postgresql://localhost:5432/artdb");
		model.getConnectionDictionary().setDriver("org.postgresql.Driver");
		model.getConnectionDictionary().setUsername("artuser");
		model.getConnectionDictionary().setPassword("artpass");
		model.getConnectionDictionary().setPlugin("PostgresqlPlugIn");

		// 2. Add an attribute to Artist
		EOEntity artist = model.getEntity("Artist");
		EOAttribute bioAttr = new EOAttribute("biography");
		bioAttr.setColumnName("BIOGRAPHY");
		bioAttr.setExternalType("text");
		bioAttr.setValueClassName("NSString");
		bioAttr.setAllowsNull(true);
		bioAttr.setClassProperty(true);
		artist.addAttribute(bioAttr);

		// 3. Create a new entity: Museum
		EOEntity museum = model.createEntity("Museum");
		museum.setExternalName("MUSEUM");
		museum.setClassName("org.objectstyle.art.Museum");

		EOAttribute museumId = new EOAttribute("museumId");
		museumId.setColumnName("MUSEUM_ID");
		museumId.setExternalType("int");
		museumId.setValueClassName("NSNumber");
		museumId.setValueType("i");
		museumId.setPrimaryKey(true);
		museumId.setUsedForLocking(true);
		museum.addAttribute(museumId);

		EOAttribute museumName = new EOAttribute("museumName");
		museumName.setColumnName("NAME");
		museumName.setExternalType("varchar");
		museumName.setValueClassName("NSString");
		museumName.setWidth(200);
		museumName.setClassProperty(true);
		museum.addAttribute(museumName);

		// 4. Delete PaintingInfo entity
		EOEntity paintingInfo = model.getEntity("PaintingInfo");
		assertNotNull(paintingInfo);
		model.removeEntity(paintingInfo);

		// 5. Save to temp folder
		File targetFolder = tempDir.resolve("art.eomodeld").toFile();
		model.saveToFolder(targetFolder);

		assertTrue(new File(targetFolder, "index.eomodeld").exists());
		assertTrue(new File(targetFolder, "Artist.plist").exists());
		assertTrue(new File(targetFolder, "Museum.plist").exists());
		assertFalse(new File(targetFolder, "PaintingInfo.plist").exists(), "PaintingInfo.plist should have been removed");

		// 6. Reload and verify all persisted data
		EOModel reloaded = new EOModel();
		reloaded.loadFromFolder(targetFolder);

		assertEquals("JDBC", reloaded.getAdaptorName());
		assertEquals("jdbc:postgresql://localhost:5432/artdb", reloaded.getConnectionDictionary().getUrl());
		assertEquals("org.postgresql.Driver", reloaded.getConnectionDictionary().getDriver());
		assertEquals("artuser", reloaded.getConnectionDictionary().getUsername());
		assertEquals("artpass", reloaded.getConnectionDictionary().getPassword());
		assertEquals("PostgresqlPlugIn", reloaded.getConnectionDictionary().getPlugin());

		assertEquals(6, reloaded.getEntities().size()); // 6 original - 1 deleted + 1 added = 6
		assertNull(reloaded.getEntity("PaintingInfo"));

		EOEntity reloadedArtist = reloaded.getEntity("Artist");
		assertNotNull(reloadedArtist);
		assertEquals(4, reloadedArtist.getAttributes().size());
		EOAttribute reloadedBio = reloadedArtist.getAttribute("biography");
		assertNotNull(reloadedBio);
		assertEquals("BIOGRAPHY", reloadedBio.getColumnName());
		assertEquals("text", reloadedBio.getExternalType());
		assertTrue(reloadedBio.isAllowsNull());
		assertTrue(reloadedBio.isClassProperty());

		EOEntity reloadedMuseum = reloaded.getEntity("Museum");
		assertNotNull(reloadedMuseum);
		assertEquals("MUSEUM", reloadedMuseum.getExternalName());
		assertEquals("org.objectstyle.art.Museum", reloadedMuseum.getClassName());
		assertEquals(2, reloadedMuseum.getAttributes().size());
		EOAttribute reloadedMuseumId = reloadedMuseum.getAttribute("museumId");
		assertTrue(reloadedMuseumId.isPrimaryKey());
		assertTrue(reloadedMuseumId.isUsedForLocking());
		EOAttribute reloadedMuseumName = reloadedMuseum.getAttribute("museumName");
		assertEquals(Integer.valueOf(200), reloadedMuseumName.getWidth());
		assertTrue(reloadedMuseumName.isClassProperty());
	}
}

