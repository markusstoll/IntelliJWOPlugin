package org.wocommunity.plugins.intellij.components;

import com.intellij.ide.SelectInContext;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.LightVirtualFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WOComponentSelectionTest {

    private Project mockProject;
    private PsiDirectory mockWoDirectory;
    private TestVirtualFile componentsDirVFile;
    private TestVirtualFile woDirVFile;
    private TestVirtualFile htmlVFile;
    private TestVirtualFile wodVFile;
    private TestVirtualFile apiVFile;
    private TestVirtualFile javaVFile;
    private DummyPsiManager dummyPsiManager;

    @BeforeEach
    public void setUp() throws Exception {
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

        componentsDirVFile = new TestVirtualFile("components", null, true, "/app/src/main/components");
        woDirVFile = new TestVirtualFile("Main.wo", componentsDirVFile, true, "/app/src/main/components/Main.wo");
        componentsDirVFile.addChild(woDirVFile);

        htmlVFile = new TestVirtualFile("Main.html", woDirVFile, false, "/app/src/main/components/Main.wo/Main.html");
        wodVFile = new TestVirtualFile("Main.wod", woDirVFile, false, "/app/src/main/components/Main.wo/Main.wod");
        woDirVFile.addChild(htmlVFile);
        woDirVFile.addChild(wodVFile);

        apiVFile = new TestVirtualFile("Main.api", componentsDirVFile, false, "/app/src/main/components/Main.api");
        componentsDirVFile.addChild(apiVFile);

        javaVFile = new TestVirtualFile("Main.java", null, false, "/app/src/main/java/Main.java");

        mockWoDirectory = mock(PsiDirectory.class);
        when(mockWoDirectory.getName()).thenReturn("Main.wo");
        when(mockWoDirectory.isDirectory()).thenReturn(true);
        when(mockWoDirectory.getVirtualFile()).thenReturn(woDirVFile);

        dummyPsiManager = new DummyPsiManager(mockProject, woDirVFile, mockWoDirectory);
        when(mockProject.getService(PsiManager.class)).thenReturn(dummyPsiManager);
        when(mockWoDirectory.getManager()).thenReturn(dummyPsiManager);
    }

    @Test
    public void testWOComponentNodeCanRepresent() {
        ViewSettings viewSettings = ViewSettings.DEFAULT;
        WOComponentNode node = new WOComponentNode(mockProject, mockWoDirectory, viewSettings);

        // Can represent itself (PsiDirectory)
        assertTrue(node.canRepresent(mockWoDirectory));

        // Can represent the .wo VirtualFile
        assertTrue(node.canRepresent(woDirVFile));

        // Can represent the inner .html file
        assertTrue(node.canRepresent(htmlVFile));

        // Can represent the inner .wod file
        assertTrue(node.canRepresent(wodVFile));

        // Can represent the sibling .api file
        assertTrue(node.canRepresent(apiVFile));

        // Cannot represent an unrelated Java file
        assertFalse(node.canRepresent(javaVFile));
    }

    @Test
    public void testWOComponentTreeStructureProviderGetTopLevelElement() {
        WOComponentTreeStructureProvider provider = new WOComponentTreeStructureProvider();

        // PsiFile inside Main.wo/Main.html
        PsiFile mockHtmlPsiFile = mock(PsiFile.class);
        when(mockHtmlPsiFile.getContainingFile()).thenReturn(mockHtmlPsiFile);
        when(mockHtmlPsiFile.getVirtualFile()).thenReturn(htmlVFile);
        when(mockHtmlPsiFile.getManager()).thenReturn(dummyPsiManager);

        assertEquals(mockWoDirectory, provider.getTopLevelElement(mockHtmlPsiFile));

        // PsiFile of Main.api
        PsiFile mockApiPsiFile = mock(PsiFile.class);
        when(mockApiPsiFile.getContainingFile()).thenReturn(mockApiPsiFile);
        when(mockApiPsiFile.getVirtualFile()).thenReturn(apiVFile);
        when(mockApiPsiFile.getManager()).thenReturn(dummyPsiManager);

        assertEquals(mockWoDirectory, provider.getTopLevelElement(mockApiPsiFile));

        // PsiFile of Main.java -> should return null
        PsiFile mockJavaPsiFile = mock(PsiFile.class);
        when(mockJavaPsiFile.getContainingFile()).thenReturn(mockJavaPsiFile);
        when(mockJavaPsiFile.getVirtualFile()).thenReturn(javaVFile);

        assertNull(provider.getTopLevelElement(mockJavaPsiFile));
    }

    @Test
    public void testWOComponentSelectInTargetCanSelect() {
        WOComponentSelectInTarget target = new WOComponentSelectInTarget();

        SelectInContext woContext = createContext(woDirVFile);
        assertTrue(target.canSelect(woContext));

        SelectInContext htmlContext = createContext(htmlVFile);
        assertTrue(target.canSelect(htmlContext));

        SelectInContext apiContext = createContext(apiVFile);
        assertTrue(target.canSelect(apiContext));

        SelectInContext javaContext = createContext(javaVFile);
        assertFalse(target.canSelect(javaContext));
    }

    private SelectInContext createContext(VirtualFile file) {
        return new com.intellij.ide.FileSelectInContext(mockProject, file);
    }

    private static class TestVirtualFile extends LightVirtualFile {
        private final VirtualFile parent;
        private final boolean isDir;
        private final String path;
        private final Map<String, VirtualFile> children = new HashMap<>();

        TestVirtualFile(String name, VirtualFile parent, boolean isDir, String path) {
            super(name);
            this.parent = parent;
            this.isDir = isDir;
            this.path = path;
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
        public String getPath() {
            return path;
        }

        @Override
        public VirtualFile findChild(String name) {
            return children.get(name);
        }
    }

    @Test
    public void testBuildArtifactDetection() {
        TestVirtualFile targetClass = new TestVirtualFile("Main.class", null, false, "/app/moduleA/target/classes/com/foo/Main.class");
        TestVirtualFile targetWo = new TestVirtualFile("Main.wo", null, true, "/app/moduleA/target/classes/Main.wo");
        TestVirtualFile buildWo = new TestVirtualFile("Main.wo", null, true, "/app/moduleA/build/resources/main/Main.wo");
        TestVirtualFile outClass = new TestVirtualFile("Main.class", null, false, "/app/moduleA/out/production/Main.class");
        TestVirtualFile srcJava = new TestVirtualFile("Main.java", null, false, "/app/moduleA/src/main/java/com/foo/Main.java");
        TestVirtualFile srcWo = new TestVirtualFile("Main.wo", null, true, "/app/moduleA/src/main/components/Main.wo");

        assertTrue(org.wocommunity.plugins.intellij.wotemplate.WOComponentClassResolver.isBuildArtifact(targetClass));
        assertTrue(org.wocommunity.plugins.intellij.wotemplate.WOComponentClassResolver.isBuildArtifact(targetWo));
        assertTrue(org.wocommunity.plugins.intellij.wotemplate.WOComponentClassResolver.isBuildArtifact(buildWo));
        assertTrue(org.wocommunity.plugins.intellij.wotemplate.WOComponentClassResolver.isBuildArtifact(outClass));

        assertFalse(org.wocommunity.plugins.intellij.wotemplate.WOComponentClassResolver.isBuildArtifact(srcJava));
        assertFalse(org.wocommunity.plugins.intellij.wotemplate.WOComponentClassResolver.isBuildArtifact(srcWo));
    }

    private static class DummyPsiManager extends PsiManager {
        private final Project project;
        private final VirtualFile targetDirVFile;
        private final PsiDirectory targetDir;

        DummyPsiManager(Project project, VirtualFile targetDirVFile, PsiDirectory targetDir) {
            this.project = project;
            this.targetDirVFile = targetDirVFile;
            this.targetDir = targetDir;
        }

        @Override
        public Project getProject() { return project; }

        @Override
        public PsiFile findFile(VirtualFile file) { return null; }

        @Override
        public PsiFile findFile(VirtualFile file, com.intellij.codeInsight.multiverse.CodeInsightContext context) { return null; }

        @Override
        public com.intellij.psi.FileViewProvider findViewProvider(VirtualFile file) { return null; }

        @Override
        public com.intellij.psi.FileViewProvider findViewProvider(VirtualFile file, com.intellij.codeInsight.multiverse.CodeInsightContext context) { return null; }

        @Override
        public PsiDirectory findDirectory(VirtualFile file) {
            if (file != null && file.equals(targetDirVFile)) {
                return targetDir;
            }
            return null;
        }

        @Override
        public boolean areElementsEquivalent(com.intellij.psi.PsiElement element1, com.intellij.psi.PsiElement element2) { return false; }

        @Override
        public void reloadFromDisk(PsiFile file) {}

        @Override
        @SuppressWarnings("deprecation")
        public void addPsiTreeChangeListener(com.intellij.psi.PsiTreeChangeListener listener) {}

        @Override
        public void addPsiTreeChangeListener(com.intellij.psi.PsiTreeChangeListener listener, com.intellij.openapi.Disposable parentDisposable) {}

        @Override
        public void addPsiTreeChangeListenerBackgroundable(com.intellij.psi.PsiTreeChangeListener listener, com.intellij.openapi.Disposable parentDisposable) {}

        @Override
        @SuppressWarnings("deprecation")
        public void removePsiTreeChangeListener(com.intellij.psi.PsiTreeChangeListener listener) {}

        @Override
        public com.intellij.psi.util.PsiModificationTracker getModificationTracker() { return null; }

        @Override
        @SuppressWarnings("deprecation")
        public void startBatchFilesProcessingMode() {}

        @Override
        @SuppressWarnings("deprecation")
        public void finishBatchFilesProcessingMode() {}

        @Override
        public <T> T runInBatchFilesMode(com.intellij.openapi.util.Computable<T> runnable) { return null; }

        @Override
        public boolean isDisposed() { return false; }

        @Override
        public void dropResolveCaches() {}

        @Override
        public void dropPsiCaches() {}

        @Override
        public boolean isInProject(com.intellij.psi.PsiElement element) { return false; }

        @Override
        public com.intellij.psi.FileViewProvider findCachedViewProvider(VirtualFile file) { return null; }
    }
}
