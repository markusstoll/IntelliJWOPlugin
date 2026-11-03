package org.wocommunity.plugins.intellij.wizards;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import javax.swing.*;
import java.io.File;
import java.util.Collections;
import java.util.List;

public class WOModuleBuilder extends ModuleBuilder {
    private static final Logger LOG = Logger.getInstance(WOModuleBuilder.class);
    private final WOProjectModel model = new WOProjectModel();

    public WOProjectModel getModel() {
        return model;
    }

    @Override
    public String getBuilderId() {
        return "WebObjects";
    }

    @Override
    public String getPresentableName() {
        return "WebObjects / Wonder";
    }

    @Override
    public String getDescription() {
        return "Create a new WebObjects or Project Wonder application or framework using WOCommunity Maven archetypes.";
    }

    @Override
    public Icon getNodeIcon() {
        return WOIcons.PLUGIN_ICON;
    }

    @Override
    public int getWeight() {
        return JVM_WEIGHT + 10;
    }

    @Override
    public ModuleType<?> getModuleType() {
        return JavaModuleType.getModuleType();
    }

    @Override
    public List<Class<? extends ModuleWizardStep>> getIgnoredSteps() {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends ModuleWizardStep> stepClass = (Class<? extends ModuleWizardStep>)
                    Class.forName("com.intellij.ide.projectWizard.ProjectSettingsStep");
            return Collections.singletonList(stepClass);
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
        return new ModuleWizardStep[]{ new WOProjectWizardStep(this, wizardContext) };
    }

    @Override
    public List<Module> commit(@NotNull Project project, ModifiableModuleModel model, ModulesProvider modulesProvider) {
        String contentEntryPath = getContentEntryPath();
        if (contentEntryPath == null || contentEntryPath.isBlank()) {
            contentEntryPath = this.model.getEffectiveLocation();
        }

        if (contentEntryPath != null && !contentEntryPath.isBlank()) {
            File targetDir = new File(contentEntryPath);
            this.model.setLocation(targetDir.getAbsolutePath());
            targetDir.mkdirs();

            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                try {
                    WOArchetypeGenerator.generate(this.model, targetDir, ProgressManager.getInstance().getProgressIndicator());
                } catch (Exception e) {
                    LOG.error("Failed to generate WebObjects project: " + e.getMessage(), e);
                }
            }, "Generating WebObjects Project...", true, project);

            VirtualFile vDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetDir);
            if (vDir != null) {
                VfsUtil.markDirtyAndRefresh(false, true, true, vDir);
            }
        }

        List<Module> modules = super.commit(project, model, modulesProvider);

        if (contentEntryPath != null && !contentEntryPath.isBlank()) {
            File targetDir = new File(contentEntryPath);
            DumbService.getInstance(project).runWhenSmart(() -> {
                WOArchetypeGenerator.importMavenProjectIfPossible(project, targetDir);
            });
        }

        return modules;
    }

    @Override
    public void setupRootModel(@NotNull ModifiableRootModel rootModel) throws ConfigurationException {
        String contentEntryPath = getContentEntryPath();
        if (contentEntryPath == null || contentEntryPath.isBlank()) {
            contentEntryPath = model.getEffectiveLocation();
        }
        if (contentEntryPath == null || contentEntryPath.isBlank()) {
            return;
        }

        File targetDir = new File(contentEntryPath);
        model.setLocation(targetDir.getAbsolutePath());
        targetDir.mkdirs();

        // If files were not generated in commit() (e.g., custom builder call), generate directly
        File pom = new File(targetDir, "pom.xml");
        if (!pom.exists()) {
            try {
                WOArchetypeGenerator.generateDirectly(model, targetDir, null);
                if (model.isAddRepositoryToPom()) {
                    WOArchetypeGenerator.addWOCommunityRepositories(new File(targetDir, "pom.xml"));
                }
            } catch (Exception e) {
                LOG.error("Fallback archetype generation failed: " + e.getMessage(), e);
            }
        }

        VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetDir);
        if (root != null) {
            VfsUtil.markDirtyAndRefresh(false, true, true, root);
            ContentEntry contentEntry = rootModel.addContentEntry(root);

            VirtualFile srcMainJava = root.findFileByRelativePath("src/main/java");
            if (srcMainJava != null) {
                contentEntry.addSourceFolder(srcMainJava, false);
            }
            VirtualFile srcMainRes = root.findFileByRelativePath("src/main/resources");
            if (srcMainRes != null) {
                contentEntry.addSourceFolder(srcMainRes, JavaResourceRootType.RESOURCE);
            }
            VirtualFile srcMainComp = root.findFileByRelativePath("src/main/components");
            if (srcMainComp != null) {
                contentEntry.addSourceFolder(srcMainComp, JavaResourceRootType.RESOURCE);
            }
            VirtualFile srcTestJava = root.findFileByRelativePath("src/test/java");
            if (srcTestJava != null) {
                contentEntry.addSourceFolder(srcTestJava, true);
            }
            VirtualFile srcTestRes = root.findFileByRelativePath("src/test/resources");
            if (srcTestRes != null) {
                contentEntry.addSourceFolder(srcTestRes, JavaResourceRootType.TEST_RESOURCE);
            }
        }

        Project project = rootModel.getProject();
        if (project.isInitialized()) {
            ApplicationManager.getApplication().invokeLater(() -> {
                WOArchetypeGenerator.importMavenProjectIfPossible(project, targetDir);
            });
        }
    }
}


