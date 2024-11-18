package org.wocommunity.plugins.intellij.actions.wocomponent;

import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOProjectUtil;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.List;

public class NewWOComponentDialog extends DialogWrapper {

    private final Project project;
    private final PsiElement selectedContext;
    private final @Nullable Module module;
    private final @Nullable PsiFile virtualFile;

    // Fields for inputs
    private TextFieldWithBrowseButton folderSelector;
    private TextFieldWithBrowseButton packageSelector;
    private TextFieldWithBrowseButton superclassSelector;
    private JBCheckBox createContent;
    private JBCheckBox createApi;
    private JTextField componentNameField;

    public NewWOComponentDialog(@Nullable Project project, @Nullable PsiElement selectedContext, @Nullable PsiFile virtualFile) {
        super(project);
        this.project = project;
        this.selectedContext = selectedContext;
        this.virtualFile = virtualFile;

        if(selectedContext != null)
            module = ModuleUtilCore.findModuleForPsiElement(selectedContext);
        else if(virtualFile != null)
            module = ModuleUtilCore.findModuleForPsiElement(virtualFile);
        else
            module = null;

        setTitle("Create WO Component");
        init();

        prefillParams();
        validateAll();
    }

    private void prefillParams() {
        // TODO restore from saved state
        folderSelector.setText(getDefaultComponentsFolder());
        packageSelector.setText(getPreselectedPackage());
        componentNameField.setText("");
        superclassSelector.setText("com.webobjects.appserver.WOComponent");
    }

    private @NlsSafe @Nullable String getDefaultComponentsFolder() {
        if(module != null)
        {
            if(WOProjectUtil.isMavenStyleModule(module))
            {
                return "src/main/components";
            } else {
                return "Components";
            }
        }

        return "src/main/components";
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JBPanel<>(new GridLayout(0, 1));
        panel.setMinimumSize(new Dimension(500, 0)); // Set minimum width

        // component name
        componentNameField = new JTextField();
        componentNameField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyPressed(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {
                validateAll();
            }
        });
        panel.add(createLabeledComponent("Name:", componentNameField));

        // Folder Selector
        folderSelector = new TextFieldWithBrowseButton();
        folderSelector.addActionListener(e -> selectFolder());
        panel.add(createLabeledComponent("Target Folder:", folderSelector));

        // Package Selector
        packageSelector = new TextFieldWithBrowseButton();
        packageSelector.addActionListener(e -> selectPackage());
        panel.add(createLabeledComponent("Target Package:", packageSelector));

        // Superclass Selector
        superclassSelector = new TextFieldWithBrowseButton();
        superclassSelector.addActionListener(e -> selectSuperclass());
        panel.add(createLabeledComponent("Superclass:", superclassSelector));

        // Checkboxes
        createContent = new JBCheckBox("Create HTML contents");
        panel.add(createContent);

        createApi = new JBCheckBox("Create API file");
        panel.add(createApi);

        return panel;
    }

    private @NlsSafe @Nullable String getPreselectedPackage() {
            if (selectedContext instanceof PsiClass) {
            // Extract the package from the PsiClass
            PsiClass psiClass = (PsiClass) selectedContext;
            PsiJavaFile containingFile = (PsiJavaFile) psiClass.getContainingFile();
            return containingFile.getPackageName();
        } else if (selectedContext instanceof PsiPackage) {
            // Directly get the package name if it is a PsiPackage
            PsiPackage psiPackage = (PsiPackage) selectedContext;
            return psiPackage.getQualifiedName();
        } else if (selectedContext instanceof PsiDirectory)
        {
            PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage((PsiDirectory) selectedContext);
            if(aPackage != null)
                return aPackage.getQualifiedName();
        } else if (selectedContext instanceof PsiFile)
        {
            PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(((PsiFile) selectedContext).getContainingDirectory());
            if(aPackage != null)
                return aPackage.getQualifiedName();
        }

        return ""; // Not a class or package
    }

    private JPanel createLabeledComponent(String label, JComponent component) {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.add(new JBLabel(label), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private void selectFolder() {
        VirtualFile preselectedFolder = WOProjectUtil.getVirtualFileForSubfolder(module, folderSelector.getText());
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        FileChooser.chooseFile(descriptor, project, preselectedFolder, new Consumer<VirtualFile>() {
            @Override
            public void consume(VirtualFile virtualFile) {
                String selectedPath = virtualFile.getPath();
                if(module != null)
                {
                    String modulePath = WOProjectUtil.getModulePath(module);
                    if(selectedPath.startsWith(modulePath))
                    {
                        folderSelector.setText(selectedPath.substring(modulePath.length() + 1));
                        validateAll();
                        return;
                    }
                }

                folderSelector.setText(selectedPath);
                validateAll();
            }
        });
    }

    private void selectPackage() {
        PackageChooserDialog dialog = new PackageChooserDialog("Select a Package", project);
        dialog.show();
        PsiPackage psiPackage = dialog.getSelectedPackage();
        if (psiPackage != null) {
            packageSelector.setText(psiPackage.getQualifiedName());
        }
    }

    private void selectSuperclass() {
        String searchQuery = JOptionPane.showInputDialog(null, "Enter superclass name to filter:");
        if (searchQuery == null || searchQuery.isBlank()) {
            return;
        }

        @Nullable PsiClass woComponentClass = JavaPsiFacade.getInstance(project).findClass("com.webobjects.appserver.WOComponent",
                GlobalSearchScope.allScope(project));

        // Search for classes matching the query
        Collection<PsiClass> classes = ClassInheritorsSearch.search(
                woComponentClass, true
        ).findAll();
        classes.add(woComponentClass);

        String[] classNames = classes.stream()
                .map(PsiClass::getQualifiedName)
                .filter(name -> name != null && name.contains(searchQuery))
                .toArray(String[]::new);

        String selectedClass = (String) JOptionPane.showInputDialog(
                null,
                "Select a superclass:",
                "Superclass Selector",
                JOptionPane.PLAIN_MESSAGE,
                null,
                classNames,
                classNames.length > 0 ? classNames[0] : null
        );

        if (selectedClass != null) {
            superclassSelector.setText(selectedClass);
        }
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return componentNameField;
    }

    public String getSelectedFolder() {
        return folderSelector.getText();
    }

    public String getSelectedPackage() {
        return packageSelector.getText();
    }

    public String getSelectedSuperclass() {
        return superclassSelector.getText();
    }

    public boolean isFeatureXEnabled() {
        return createContent.isSelected();
    }

    public boolean isFeatureYEnabled() {
        return createApi.isSelected();
    }

    @Override
    protected @NotNull List<ValidationInfo> doValidateAll() {
        return super.doValidateAll();
    }

    protected void validateAll()
    {
        boolean isEnabled = true;

        isEnabled &= componentNameField.getText().length() > 0;

        String componentPath = folderSelector.getText();
        isEnabled &= !componentPath.startsWith("/");
        isEnabled &= !componentPath.endsWith(".wo");

        getOKAction().setEnabled(isEnabled);
    }

    @Override
    protected void doOKAction() {
        if (getOKAction().isEnabled()) {
            VirtualFile selectedFolder = WOProjectUtil.getVirtualFileForSubfolder(module, folderSelector.getText());

            // Use WriteCommandAction for PSI modifications
            ApplicationManager.getApplication().invokeLater(() -> {
                WriteCommandAction.runWriteCommandAction(module.getProject(), () -> {
                    try {
                        WOProjectUtil.createNewWOComponent(
                                module,
                                componentNameField.getText(),
                                packageSelector.getText(),
                                superclassSelector.getText(),
                                selectedFolder,
                                createContent.isSelected(),
                                createApi.isSelected()
                        );

//                        JOptionPane.showMessageDialog(null, "File created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Error creating file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            });

            close(OK_EXIT_CODE);
        }
    }
}
