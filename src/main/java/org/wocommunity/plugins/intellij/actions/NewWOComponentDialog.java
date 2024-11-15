package org.wocommunity.plugins.intellij.actions;

import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class NewWOComponentDialog extends DialogWrapper {

    private final Project project;
    private final PsiElement selectedContext;

    // Fields for inputs
    private TextFieldWithBrowseButton folderSelector;
    private TextFieldWithBrowseButton packageSelector;
    private TextFieldWithBrowseButton superclassSelector;
    private JBCheckBox checkbox1;
    private JBCheckBox checkbox2;

    public NewWOComponentDialog(@Nullable Project project, @Nullable PsiElement selectedContext) {
        super(project);
        this.project = project;
        this.selectedContext = selectedContext;
        setTitle("Create WO Component");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JBPanel<>(new GridLayout(6, 1, 10, 10));
        panel.setMinimumSize(new Dimension(500, 200)); // Set minimum width

        // Folder Selector
        folderSelector = new TextFieldWithBrowseButton();
        folderSelector.setText("Select a folder");
        folderSelector.addActionListener(e -> selectFolder());
        panel.add(createLabeledComponent("Target Folder:", folderSelector));

        // Package Selector
        packageSelector = new TextFieldWithBrowseButton();
        packageSelector.setText(getPreselectedPackage());
        packageSelector.addActionListener(e -> selectPackage());
        panel.add(createLabeledComponent("Target Package:", packageSelector));

        // Superclass Selector
        superclassSelector = new TextFieldWithBrowseButton();
        superclassSelector.setText("com.webobjects.appserver.WOComponent");
        superclassSelector.addActionListener(e -> selectSuperclass());
        panel.add(createLabeledComponent("Superclass:", superclassSelector));

        // Checkboxes
        checkbox1 = new JBCheckBox("Enable feature X");
        checkbox2 = new JBCheckBox("Enable feature Y");
        panel.add(checkbox1);
        panel.add(checkbox2);

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
        }

        return ""; // Not a class or package
    }

    private JPanel createLabeledComponent(String label, JComponent component) {
        JPanel panel = new JBPanel<>(new BorderLayout());
        panel.add(new JBLabel(label), BorderLayout.WEST);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private void selectFolder() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        FileChooser.chooseFile(descriptor, project, null, new Consumer<VirtualFile>() {
            @Override
            public void consume(VirtualFile virtualFile) {
                folderSelector.setText(virtualFile.getPath());
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
        return folderSelector;
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
        return checkbox1.isSelected();
    }

    public boolean isFeatureYEnabled() {
        return checkbox2.isSelected();
    }
}
