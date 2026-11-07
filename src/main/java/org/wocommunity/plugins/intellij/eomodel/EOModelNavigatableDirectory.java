package org.wocommunity.plugins.intellij.eomodel;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wocommunity.plugins.intellij.tools.WOIcons;

import javax.swing.*;

public class EOModelNavigatableDirectory implements PsiDirectory {

	private final PsiDirectory delegate;

	public EOModelNavigatableDirectory(@NotNull PsiDirectory delegate) {
		this.delegate = delegate;
	}

	public @NotNull PsiDirectory getDelegate() {
		return delegate;
	}

	@Override
	public boolean canNavigate() {
		return true;
	}

	@Override
	public boolean canNavigateToSource() {
		return true;
	}

	@Override
	public void navigate(boolean requestFocus) {
		VirtualFile vFile = getVirtualFile();
		if (vFile != null) {
			EOModelEditorUtils.openModel(getProject(), vFile);
		}
		try {
			delegate.navigate(false);
		} catch (Exception ignored) {
		}
	}

	@Override
	public @NotNull VirtualFile getVirtualFile() {
		return delegate.getVirtualFile();
	}

	@Override
	public @NotNull @NlsSafe String getName() {
		return delegate.getName();
	}

	@Override
	public PsiElement setName(@NotNull @NlsSafe String name) throws IncorrectOperationException {
		return delegate.setName(name);
	}

	@Override
	public @Nullable PsiDirectory getParentDirectory() {
		return delegate.getParentDirectory();
	}

	@Override
	public @NotNull PsiDirectory[] getSubdirectories() {
		return delegate.getSubdirectories();
	}

	@Override
	public @NotNull PsiFile[] getFiles() {
		return delegate.getFiles();
	}

	@Override
	public @Nullable PsiDirectory findSubdirectory(@NotNull String name) {
		return delegate.findSubdirectory(name);
	}

	@Override
	public @Nullable PsiFile findFile(@NotNull @NlsSafe String name) {
		return delegate.findFile(name);
	}

	@Override
	public @NotNull PsiDirectory createSubdirectory(@NotNull String name) throws IncorrectOperationException {
		return delegate.createSubdirectory(name);
	}

	@Override
	public void checkCreateSubdirectory(@NotNull String name) throws IncorrectOperationException {
		delegate.checkCreateSubdirectory(name);
	}

	@Override
	public @NotNull PsiFile createFile(@NotNull @NlsSafe String name) throws IncorrectOperationException {
		return delegate.createFile(name);
	}

	@Override
	public @NotNull PsiFile copyFileFrom(@NotNull String newName, @NotNull PsiFile originalFile) throws IncorrectOperationException {
		return delegate.copyFileFrom(newName, originalFile);
	}

	@Override
	public boolean isDirectory() {
		return delegate.isDirectory();
	}

	@Override
	public void checkCreateFile(@NotNull String name) throws IncorrectOperationException {
		delegate.checkCreateFile(name);
	}

	@Override
	public @NotNull PsiElement copy() {
		return delegate.copy();
	}

	@Override
	public PsiElement add(@NotNull PsiElement element) throws IncorrectOperationException {
		return delegate.add(element);
	}

	@Override
	public PsiElement addBefore(@NotNull PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException {
		return delegate.addBefore(element, anchor);
	}

	@Override
	public PsiElement addAfter(@NotNull PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException {
		return delegate.addAfter(element, anchor);
	}

	@Override
	public void checkAdd(@NotNull PsiElement element) throws IncorrectOperationException {
		delegate.checkAdd(element);
	}

	@Override
	public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
		return delegate.addRange(first, last);
	}

	@Override
	public PsiElement addRangeBefore(@NotNull PsiElement first, @NotNull PsiElement last, PsiElement anchor) throws IncorrectOperationException {
		return delegate.addRangeBefore(first, last, anchor);
	}

	@Override
	public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
		return delegate.addRangeAfter(first, last, anchor);
	}

	@Override
	public void delete() throws IncorrectOperationException {
		delegate.delete();
	}

	@Override
	public void checkDelete() throws IncorrectOperationException {
		delegate.checkDelete();
	}

	@Override
	public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
		delegate.deleteChildRange(first, last);
	}

	@Override
	public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
		return delegate.replace(newElement);
	}

	@Override
	public @NotNull Project getProject() throws PsiInvalidElementAccessException {
		return delegate.getProject();
	}

	@Override
	public @NotNull Language getLanguage() {
		return delegate.getLanguage();
	}

	@Override
	public PsiManager getManager() {
		return delegate.getManager();
	}

	@Override
	public @NotNull PsiElement[] getChildren() {
		return delegate.getChildren();
	}

	@Override
	public PsiDirectory getParent() {
		return delegate.getParent();
	}

	@Override
	public PsiElement getFirstChild() {
		return delegate.getFirstChild();
	}

	@Override
	public PsiElement getLastChild() {
		return delegate.getLastChild();
	}

	@Override
	public PsiElement getNextSibling() {
		return delegate.getNextSibling();
	}

	@Override
	public PsiElement getPrevSibling() {
		return delegate.getPrevSibling();
	}

	@Override
	public PsiFile getContainingFile() throws PsiInvalidElementAccessException {
		return delegate.getContainingFile();
	}

	@Override
	public TextRange getTextRange() {
		return delegate.getTextRange();
	}

	@Override
	public int getStartOffsetInParent() {
		return delegate.getStartOffsetInParent();
	}

	@Override
	public int getTextLength() {
		return delegate.getTextLength();
	}

	@Override
	public @Nullable PsiElement findElementAt(int offset) {
		return delegate.findElementAt(offset);
	}

	@Override
	public @Nullable PsiReference findReferenceAt(int offset) {
		return delegate.findReferenceAt(offset);
	}

	@Override
	public int getTextOffset() {
		return delegate.getTextOffset();
	}

	@Override
	public @NlsSafe String getText() {
		return delegate.getText();
	}

	@Override
	public @NotNull char[] textToCharArray() {
		return delegate.textToCharArray();
	}

	@Override
	public PsiElement getNavigationElement() {
		return this;
	}

	@Override
	public PsiElement getOriginalElement() {
		return delegate.getOriginalElement();
	}

	@Override
	public boolean textMatches(@NotNull @NlsSafe CharSequence text) {
		return delegate.textMatches(text);
	}

	@Override
	public boolean textMatches(@NotNull PsiElement element) {
		return delegate.textMatches(element);
	}

	@Override
	public boolean textContains(char c) {
		return delegate.textContains(c);
	}

	@Override
	public void accept(@NotNull PsiElementVisitor visitor) {
		delegate.accept(visitor);
	}

	@Override
	public void acceptChildren(@NotNull PsiElementVisitor visitor) {
		delegate.acceptChildren(visitor);
	}

	@Override
	public boolean isValid() {
		return delegate.isValid();
	}

	@Override
	public boolean isWritable() {
		return delegate.isWritable();
	}

	@Override
	public @Nullable PsiReference getReference() {
		return delegate.getReference();
	}

	@Override
	public @NotNull PsiReference[] getReferences() {
		return delegate.getReferences();
	}

	@Override
	public <T> @Nullable T getCopyableUserData(@NotNull Key<T> key) {
		return delegate.getCopyableUserData(key);
	}

	@Override
	public <T> void putCopyableUserData(@NotNull Key<T> key, @Nullable T value) {
		delegate.putCopyableUserData(key, value);
	}

	@Override
	public boolean processChildren(@NotNull com.intellij.psi.search.PsiElementProcessor<? super PsiFileSystemItem> processor) {
		return delegate.processChildren(processor);
	}

	@Override
	public boolean processDeclarations(@NotNull PsiScopeProcessor processor, @NotNull ResolveState state, @Nullable PsiElement lastParent, @NotNull PsiElement place) {
		return delegate.processDeclarations(processor, state, lastParent, place);
	}

	@Override
	public void checkSetName(String name) throws IncorrectOperationException {
		delegate.checkSetName(name);
	}

	@Override
	public @Nullable PsiElement getContext() {
		return delegate.getContext();
	}

	@Override
	public boolean isPhysical() {
		return delegate.isPhysical();
	}

	@Override
	public @NotNull GlobalSearchScope getResolveScope() {
		return delegate.getResolveScope();
	}

	@Override
	public @NotNull SearchScope getUseScope() {
		return delegate.getUseScope();
	}

	@Override
	public ASTNode getNode() {
		return delegate.getNode();
	}

	@Override
	public boolean isEquivalentTo(PsiElement another) {
		if (another instanceof EOModelNavigatableDirectory) {
			return delegate.isEquivalentTo(((EOModelNavigatableDirectory) another).delegate);
		}
		return delegate.isEquivalentTo(another);
	}

	@Override
	public Icon getIcon(int flags) {
		return WOIcons.EOMODEL_ICON;
	}

	@Override
	public <T> @Nullable T getUserData(@NotNull Key<T> key) {
		return delegate.getUserData(key);
	}

	@Override
	public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
		delegate.putUserData(key, value);
	}

	@Override
	public ItemPresentation getPresentation() {
		return new ItemPresentation() {
			@Override
			public @NlsSafe @Nullable String getPresentableText() {
				String name = delegate.getName();
				if (name.endsWith(".eomodeld")) {
					return name.substring(0, name.length() - ".eomodeld".length()) + " EOModel";
				}
				return name;
			}

			@Override
			public @Nullable Icon getIcon(boolean unused) {
				return WOIcons.EOMODEL_ICON;
			}
		};
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof EOModelNavigatableDirectory that) {
			return delegate.equals(that.delegate);
		}
		return delegate.equals(obj);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public String toString() {
		return "EOModelNavigatableDirectory: " + delegate.getName();
	}
}
