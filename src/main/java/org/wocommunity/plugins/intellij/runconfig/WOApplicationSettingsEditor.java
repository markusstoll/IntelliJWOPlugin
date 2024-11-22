package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ui.*;
import com.intellij.execution.application.ClassEditorField;
import com.intellij.execution.application.JavaSettingsEditorBase;
import com.intellij.ide.util.ClassFilter;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.PsiMethodUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.runconfig.data.KeyValueOptionTableModel;
import org.wocommunity.plugins.intellij.runconfig.data.VMParametersTableModel;

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

        KeyValueOptionsFragment woOptionsFragment = new KeyValueOptionsFragment(new KeyValueOptionTableModel(mySettings.getOptions().getWoOptions()));
        setMinimumWidth(woOptionsFragment.getEditorComponent(), 600);
        fragments.add(woOptionsFragment);

        VMOptionsFragment vmOptionsFragment = new VMOptionsFragment(new VMParametersTableModel(mySettings.getOptions().getHigherJdkVMParameters()));
        setMinimumWidth(vmOptionsFragment.getEditorComponent(), 600);
        fragments.add(vmOptionsFragment);
    }

    private static final ClassFilter WO_CLASS_FILTER =
            aClass -> PsiMethodUtil.MAIN_CLASS.value(aClass)
                    && superClassIsWOApplication(aClass)
                    && ReadAction.compute(() -> PsiMethodUtil.findMainMethod(aClass)) != null;

    private static boolean superClassIsWOApplication(PsiClass aClass) {
        PsiClass superClass = aClass.getSuperClass();
        if(superClass != null)
        {
            if(superClass.getQualifiedName().equals("com.webobjects.appserver.WOApplication"))
                return true;

            return superClassIsWOApplication(superClass);
        }

        return false;
    }

    @NotNull
    private SettingsEditorFragment<WOApplicationConfiguration, EditorTextField> createMainClass(ModuleClasspathCombo classpathCombo) {
        EditorTextField mainClass = ClassEditorField.createClassField(getProject(),
                () -> classpathCombo.getSelectedModule(),
                JavaCodeFragment.VisibilityChecker.PROJECT_SCOPE_VISIBLE,
                new ClassBrowser.AppClassBrowser<>(getProject(), () -> classpathCombo.getSelectedModule()) {
                    @Override
                    protected ClassFilter createFilter(Module module) {
                        return WO_CLASS_FILTER;
                    }
                }
                );
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