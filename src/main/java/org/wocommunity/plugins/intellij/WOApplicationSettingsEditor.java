package org.wocommunity.plugins.intellij;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ui.*;
import com.intellij.execution.application.ClassEditorField;
import com.intellij.execution.application.JavaSettingsEditorBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.execution.ui.CommandLinePanel.setMinimumWidth;

public class WOApplicationSettingsEditor extends JavaSettingsEditorBase<WOApplicationConfiguration> {

    public WOApplicationSettingsEditor(WOApplicationConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected void customizeFragments(List<SettingsEditorFragment<WOApplicationConfiguration, ?>> fragments,
                                      SettingsEditorFragment<WOApplicationConfiguration, ModuleClasspathCombo> moduleClasspath,
                                      CommonParameterFragments<WOApplicationConfiguration> commonParameterFragments) {
//        fragments.add(SettingsEditorFragment.createTag("include.provided",
//                ExecutionBundle.message("application.configuration.include.provided.scope"),
//                ExecutionBundle.message("group.java.options"),
//                configuration -> configuration.getOptions().isIncludeProvidedScope(),
//                (configuration, value) -> configuration.getOptions().setIncludeProvidedScope(value)));
        fragments.add(commonParameterFragments.programArguments());
//        fragments.add(new TargetPathFragment<>());
//        fragments.add(commonParameterFragments.createRedirectFragment());
        SettingsEditorFragment<WOApplicationConfiguration, EditorTextField> mainClassFragment = createMainClass(moduleClasspath.component());
        fragments.add(mainClassFragment);
        DefaultJreSelector jreSelector = DefaultJreSelector.fromSourceRootsDependencies(moduleClasspath.component(), mainClassFragment.component());
        SettingsEditorFragment<WOApplicationConfiguration, JrePathEditor> jrePath = CommonJavaFragments.createJrePath(jreSelector);
        fragments.add(jrePath);
        fragments.add(createShortenClasspath(moduleClasspath.component(), jrePath, true));

        fragments.add(new KeyValueOptionsFragment(this.mySettings));
    }

    @NotNull
    private SettingsEditorFragment<WOApplicationConfiguration, EditorTextField> createMainClass(ModuleClasspathCombo classpathCombo) {
        EditorTextField mainClass = ClassEditorField.createClassField(getProject(), () -> classpathCombo.getSelectedModule(),
                JavaCodeFragment.VisibilityChecker.PROJECT_SCOPE_VISIBLE, null);
        mainClass.setBackground(UIUtil.getTextFieldBackground());
        mainClass.setShowPlaceholderWhenFocused(true);
        CommonParameterFragments.setMonospaced(mainClass);
        String placeholder = ExecutionBundle.message("application.configuration.main.class.placeholder");
        mainClass.setPlaceholder(placeholder);
        mainClass.getAccessibleContext().setAccessibleName(placeholder);
        setMinimumWidth(mainClass, 300);
        SettingsEditorFragment<WOApplicationConfiguration, EditorTextField> mainClassFragment =
                new SettingsEditorFragment<>("mainClass", ExecutionBundle.message("application.configuration.main.class"), null, mainClass, 20,
                        (configuration, component) -> component.setText(configuration.getMainClassName()),
                        (configuration, component) -> configuration.setMainClassName(component.getText()),
                        configuration -> true);
        mainClassFragment.setHint(ExecutionBundle.message("application.configuration.main.class.hint"));
        mainClassFragment.setRemovable(false);
        mainClassFragment.setEditorGetter(field -> {
            Editor editor = field.getEditor();
            return editor == null ? field : editor.getContentComponent();
        });

        return mainClassFragment;
    }
}