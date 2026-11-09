package org.wocommunity.plugins.intellij.eomodel;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wocommunity.plugins.intellij.eogen.EOGenNode;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EOModelTreeStructureProviderTest {

	private Project mockProject;
	private ViewSettings mockSettings;

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

	private static class DummyNode extends AbstractTreeNode<Object> {
		DummyNode(Project project, Object value) {
			super(project, value);
		}

		@Override
		public @NotNull Collection<? extends AbstractTreeNode<?>> getChildren() {
			return Collections.emptyList();
		}

		@Override
		protected void update(@NotNull PresentationData presentation) {
		}
	}

	@BeforeEach
	public void setUp() {
		try {
			java.lang.reflect.Field ourInstance = com.intellij.ide.util.treeView.TreeAnchorizer.class.getDeclaredField("ourInstance");
			ourInstance.setAccessible(true);
			if (ourInstance.get(null) == null) {
				java.lang.reflect.Constructor<?> ctor = com.intellij.ide.util.treeView.TreeAnchorizer.class.getDeclaredConstructor();
				ctor.setAccessible(true);
				ourInstance.set(null, ctor.newInstance());
			}
		} catch (Exception ignored) {
		}

		mockProject = mock(Project.class);
		mockSettings = ViewSettings.DEFAULT;
	}

	@Test
	public void testModifyWrapsEomodeldAndEogen() {
		EOModelTreeStructureProvider provider = new EOModelTreeStructureProvider();

		// Create .eomodeld directory
		TestVirtualFile modelVf = new TestVirtualFile("MyModel.eomodeld", null, true);
		TestVirtualFile indexVf = new TestVirtualFile("index.eomodeld", modelVf, false);
		modelVf.addChild(indexVf);

		PsiDirectory modelDir = mock(PsiDirectory.class);
		when(modelDir.getVirtualFile()).thenReturn(modelVf);

		DummyNode modelChild = new DummyNode(mockProject, modelDir);

		// Create .eogen file
		TestVirtualFile eogenVf = new TestVirtualFile("MyModel.eogen", null, false);
		PsiFile eogenFile = mock(PsiFile.class);
		when(eogenFile.getVirtualFile()).thenReturn(eogenVf);

		DummyNode eogenChild = new DummyNode(mockProject, eogenFile);

		// Normal file
		TestVirtualFile normalVf = new TestVirtualFile("app.properties", null, false);
		PsiFile normalFile = mock(PsiFile.class);
		when(normalFile.getVirtualFile()).thenReturn(normalVf);

		DummyNode normalChild = new DummyNode(mockProject, normalFile);

		DummyNode parentNode = new DummyNode(mockProject, new Object());
		Collection<AbstractTreeNode<?>> result = provider.modify(
				parentNode,
				List.of(modelChild, eogenChild, normalChild),
				mockSettings
		);

		assertEquals(3, result.size());
		List<AbstractTreeNode<?>> resultList = List.copyOf(result);

		assertTrue(resultList.get(0) instanceof EOModelNode, "MyModel.eomodeld must be wrapped in EOModelNode");
		assertTrue(resultList.get(1) instanceof EOGenNode, "MyModel.eogen must be wrapped in EOGenNode");
		assertSame(normalChild, resultList.get(2), "Normal file should not be modified");

		// Verify weights match so they sort together at the bottom
		assertEquals(30, resultList.get(0).getWeight());
		assertEquals(30, resultList.get(1).getWeight());
	}

	@Test
	public void testGetTopLevelElementForEogenFile() {
		EOModelTreeStructureProvider provider = new EOModelTreeStructureProvider();

		TestVirtualFile eogenVf = new TestVirtualFile("MyModel.eogen", null, false);
		PsiFile eogenFile = mock(PsiFile.class);
		when(eogenFile.getContainingFile()).thenReturn(eogenFile);
		when(eogenFile.getVirtualFile()).thenReturn(eogenVf);

		assertEquals(eogenFile, provider.getTopLevelElement(eogenFile));
	}
}
