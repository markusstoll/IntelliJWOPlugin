package org.wocommunity.plugins.intellij.eogen;

import org.junit.jupiter.api.Test;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EOGenFileTypeTest {

	@Test
	public void testFileTypeProperties() {
		EOGenFileType fileType = EOGenFileType.INSTANCE;
		assertEquals("EOGenerator", fileType.getName());
		assertEquals("eogen", fileType.getDefaultExtension());
		assertNotNull(fileType.getIcon());
		assertEquals(WOIcons.EOGEN_ICON, fileType.getIcon());
	}

	@Test
	public void testIconsLoaded() {
		assertNotNull(WOIcons.EOMODEL_ICON, "EOModel icon must be loaded");
		assertNotNull(WOIcons.EOGEN_ICON, "EOGen icon must be loaded");
	}
}
