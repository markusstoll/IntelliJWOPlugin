package org.wocommunity.plugins.intellij.eomodel;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class EOModelFileUtilTest {

	private static class TestVirtualFile extends LightVirtualFile {
		private final VirtualFile parent;
		private final boolean isDir;
		private final Map<String, VirtualFile> children = new HashMap<>();

		TestVirtualFile(String name, VirtualFile parent, boolean isDir) {
			super(name);
			this.parent = parent;
			this.isDir = isDir;
		}

		void addChild(VirtualFile child) {
			children.put(child.getName(), child);
		}

		@Override
		public VirtualFile getParent() {
			return parent;
		}

		@Override
		public boolean isDirectory() {
			return isDir;
		}

		@Override
		public VirtualFile findChild(@NotNull String name) {
			return children.get(name);
		}
	}

	@Test
	public void testModelFolderDetection() {
		TestVirtualFile modelDir = new TestVirtualFile("Main.eomodeld", null, true);
		assertTrue(EOModelFileUtil.fileIsModel(modelDir));
		assertEquals("Main", EOModelFileUtil.getModelName(modelDir));
		assertSame(modelDir, EOModelFileUtil.getModel(modelDir));

		TestVirtualFile indexFile = new TestVirtualFile("index.eomodeld", modelDir, false);
		modelDir.addChild(indexFile);
		assertFalse(EOModelFileUtil.fileIsModel(indexFile));
		assertTrue(EOModelFileUtil.fileIsInModel(indexFile));
		assertSame(modelDir, EOModelFileUtil.getModel(indexFile));
		assertEquals("Main", EOModelFileUtil.getModelName(indexFile));

		TestVirtualFile entityFile = new TestVirtualFile("User.plist", modelDir, false);
		modelDir.addChild(entityFile);
		assertFalse(EOModelFileUtil.fileIsModel(entityFile));
		assertTrue(EOModelFileUtil.fileIsInModel(entityFile));
		assertSame(modelDir, EOModelFileUtil.getModel(entityFile));
		assertEquals("Main", EOModelFileUtil.getModelName(entityFile));

		TestVirtualFile otherFile = new TestVirtualFile("Main.java", null, false);
		assertFalse(EOModelFileUtil.fileIsModel(otherFile));
		assertFalse(EOModelFileUtil.fileIsInModel(otherFile));
		assertNull(EOModelFileUtil.getModel(otherFile));
	}

	@Test
	public void testEditorProviderAccept() {
		EOModelEditorProvider provider = new EOModelEditorProvider();
		Project project = mock(Project.class);

		TestVirtualFile modelDir = new TestVirtualFile("Main.eomodeld", null, true);
		assertTrue(provider.accept(project, modelDir));

		TestVirtualFile indexFile = new TestVirtualFile("index.eomodeld", modelDir, false);
		assertTrue(provider.accept(project, indexFile));

		TestVirtualFile randomFile = new TestVirtualFile("Test.java", null, false);
		assertFalse(provider.accept(project, randomFile));
	}

	@Test
	public void testEditorTabTitleProvider() {
		EOModelEditorTabTitleProvider provider = new EOModelEditorTabTitleProvider();
		Project project = mock(Project.class);

		TestVirtualFile modelDir = new TestVirtualFile("Main.eomodeld", null, true);
		assertEquals("Main", provider.getEditorTabTitle(project, modelDir));

		TestVirtualFile indexFile = new TestVirtualFile("index.eomodeld", modelDir, false);
		assertEquals("Main", provider.getEditorTabTitle(project, indexFile));
	}
}
