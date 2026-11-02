package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.execution.ui.*;
import com.intellij.execution.application.ClassEditorField;
import com.intellij.execution.application.JavaSettingsEditorBase;
import com.intellij.ide.util.ClassFilter;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
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
//                "Include dependencies with \"Provided\" scope",
//                "Java options",
//                configuration -> configuration.getOptions().isIncludeProvidedScope(),
//                (configuration, value) -> configuration.getOptions().setIncludeProvidedScope(value)));
        fragments.add(commonParameterFragments.programArguments());
//        fragments.add(new TargetPathFragment<>());
//        fragments.add(commonParameterFragments.createRedirectFragment());
        moduleClasspath.component().addActionListener(e -> {
            Module selectedModule = moduleClasspath.component().getSelectedModule();
            if (mySettings != null) {
                if (selectedModule != null) {
                    mySettings.setModule(selectedModule);
                } else {
                    mySettings.getConfigurationModule().setModule(null);
                    mySettings.getConfigurationModule().setModuleName(null);
                }
                if (mySettings.isGeneratedName()) {
                    String newName = mySettings.suggestedName();
                    if (newName != null && !newName.isBlank()) {
                        mySettings.setName(newName);
                    }
                }
            }
            fireEditorStateChanged();
        });
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

    private static final ClassFilter WO_CLASS_FILTER = WOApplicationClassUtil::isCandidateMainClass;

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

                    @Override
                    protected TreeClassChooser createClassChooser(ClassFilter.ClassFilterWithScope filterWithScope) {
                        Project project = getProject();
                        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
                        return TreeClassChooserFactory.getInstance(project)
                                .createWithInnerClassesScopeChooser(myTitle, scope, WO_CLASS_FILTER, null);
                    }

                    @Override
                    protected void onClassChosen(PsiClass aClass) {
                        super.onClassChosen(aClass);
                        if (aClass != null) {
                            Module module = ModuleUtilCore.findModuleForPsiElement(aClass);
                            if (module != null) {
                                classpathCombo.setSelectedModule(module);
                            }
                        }
                    }
                }
                );
        mainClass.setBackground(UIUtil.getTextFieldBackground());
        mainClass.setShowPlaceholderWhenFocused(true);
        CommonParameterFragments.setMonospaced(mainClass);
        String placeholder = "none";
        mainClass.setPlaceholder(placeholder);
        mainClass.getAccessibleContext().setAccessibleName(placeholder);
        setMinimumWidth(mainClass, 300);
        SettingsEditorFragment<WOApplicationConfiguration, EditorTextField> mainClassFragment =
                new SettingsEditorFragment<>("mainClass", "Main class", null, mainClass, 20,
                        (configuration, component) -> component.setText(configuration.getMainClassName()),
                        (configuration, component) -> configuration.setMainClassName(component.getText()),
                        configuration -> true);
        mainClassFragment.setHint("Specify the fully qualified name of the Application class");
        mainClassFragment.setRemovable(false);
        mainClassFragment.setEditorGetter(field -> {
            Editor editor = field.getEditor();
            return editor == null ? field : editor.getContentComponent();
        });

        return mainClassFragment;
    }
}