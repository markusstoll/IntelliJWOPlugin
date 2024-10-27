package org.wocommunity.plugins.intellij;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class WORunConfigurationProducer extends LazyRunConfigurationProducer<WOApplicationConfiguration> {
    private final List<String> imports = List.of("org.openqa.selenium.support.FindBy", "org.openqa.selenium.WebElement", "org.openqa.selenium.By");

    @NotNull
    @Override
    public ConfigurationFactory getConfigurationFactory() {
        return new RunConfigurationFactory(WORunConfigType.getInstance());
    }

    @Override
    protected boolean setupConfigurationFromContext(@NotNull WOApplicationConfiguration configuration, @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
        var psiLocation = context.getPsiLocation();

        if (psiLocation == null || (psiLocation != null && !psiLocation.getLanguage().is(JavaLanguage.INSTANCE))) {
            return false;
        }

        var containingFile = (PsiJavaFile) psiLocation.getContainingFile();
        var importList = containingFile.getImportList();
        if (importList == null) {
            return false;
        }

        sourceElement.set(containingFile.getClasses()[0]);

        configuration.setName(containingFile.getClasses()[0].getName());

        return Arrays.stream(importList.getImportStatements()).anyMatch(psiImportStatement -> imports.contains(psiImportStatement.getQualifiedName()));
    }

    @Override
    public boolean isConfigurationFromContext(@NotNull WOApplicationConfiguration configuration, @NotNull ConfigurationContext context) {
        var psiLocation = context.getPsiLocation();

        if (psiLocation == null || (psiLocation != null && !psiLocation.getLanguage().is(JavaLanguage.INSTANCE))) {
            return false;
        }

        var containingFile = (PsiJavaFile) psiLocation.getContainingFile();
        return Arrays.stream(containingFile.getClasses()).anyMatch(psiClass -> Objects.equals(psiClass.getQualifiedName(), this));
    }

    private class RunConfigurationFactory extends ConfigurationFactory {
        public RunConfigurationFactory(WORunConfigType type) {
            super(type);
        }

        @Override
        public @NotNull String getId() {
            return WORunConfigType.ID;
        }

        @NotNull
        @Override
        public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return new WOApplicationConfiguration("WO", this, project);
        }

        @Override
        public Class<? extends BaseState> getOptionsClass() {
            return WORunConfigurationOptions.class;
        }
    }
}