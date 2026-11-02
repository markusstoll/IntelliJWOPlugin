package org.wocommunity.plugins.intellij.wotemplate;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.*;
import com.intellij.testFramework.LightVirtualFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WOComponentBindingResolutionTest {

    @Test
    public void testGetComponentNameFromVirtualFile() {
        LightVirtualFile woDir = new LightVirtualFile("Main.wo");
        assertEquals("Main", WOComponentClassResolver.getComponentName(woDir));

        LightVirtualFile htmlFile = new LightVirtualFile("Main.html");
        assertEquals("Main", WOComponentClassResolver.getComponentName(htmlFile));

        LightVirtualFile wodFile = new LightVirtualFile("Main.wod");
        assertEquals("Main", WOComponentClassResolver.getComponentName(wodFile));

        LightVirtualFile complexName = new LightVirtualFile("UserDetailEditor.wo");
        assertEquals("UserDetailEditor", WOComponentClassResolver.getComponentName(complexName));
    }

    @Test
    public void testIsComponentClassByNameHierarchy() {
        PsiClass mockComponent = mock(PsiClass.class);
        when(mockComponent.getName()).thenReturn("Main");
        PsiClass mockSuperClass = mock(PsiClass.class);
        when(mockSuperClass.getName()).thenReturn("ERXComponent");
        when(mockComponent.getSuperClass()).thenReturn(mockSuperClass);
        when(mockComponent.getProject()).thenReturn(mock(com.intellij.openapi.project.Project.class));

        assertTrue(WOComponentClassResolver.isComponentClass(mockComponent));
    }

    @Test
    public void testFindSingleKeyTargetsGetter() {
        PsiClass mockClass = mock(PsiClass.class);
        PsiMethod getTitleMethod = mock(PsiMethod.class);
        when(getTitleMethod.getName()).thenReturn("getTitle");
        PsiParameterList emptyParams = mock(PsiParameterList.class);
        when(emptyParams.isEmpty()).thenReturn(true);
        when(getTitleMethod.getParameterList()).thenReturn(emptyParams);

        when(mockClass.getAllMethods()).thenReturn(new PsiMethod[]{getTitleMethod});
        when(mockClass.getAllFields()).thenReturn(new PsiField[0]);

        List<PsiElement> targets = WOComponentClassResolver.findSingleKeyTargets(mockClass, "title");
        assertFalse(targets.isEmpty());
        assertEquals(getTitleMethod, targets.get(0));
    }

    @Test
    public void testFindSingleKeyTargetsBooleanGetter() {
        PsiClass mockClass = mock(PsiClass.class);
        PsiMethod isEnabledMethod = mock(PsiMethod.class);
        when(isEnabledMethod.getName()).thenReturn("isEnabled");
        PsiParameterList emptyParams = mock(PsiParameterList.class);
        when(emptyParams.isEmpty()).thenReturn(true);
        when(isEnabledMethod.getParameterList()).thenReturn(emptyParams);

        when(mockClass.getAllMethods()).thenReturn(new PsiMethod[]{isEnabledMethod});
        when(mockClass.getAllFields()).thenReturn(new PsiField[0]);

        List<PsiElement> targets = WOComponentClassResolver.findSingleKeyTargets(mockClass, "enabled");
        assertFalse(targets.isEmpty());
        assertEquals(isEnabledMethod, targets.get(0));

        // Direct method name match (e.g. $isEnabled)
        List<PsiElement> directTargets = WOComponentClassResolver.findSingleKeyTargets(mockClass, "isEnabled");
        assertFalse(directTargets.isEmpty());
        assertEquals(isEnabledMethod, directTargets.get(0));
    }

    @Test
    public void testFindSingleKeyTargetsActionMethod() {
        PsiClass mockClass = mock(PsiClass.class);
        PsiMethod saveMethod = mock(PsiMethod.class);
        when(saveMethod.getName()).thenReturn("save");
        PsiParameterList emptyParams = mock(PsiParameterList.class);
        when(emptyParams.isEmpty()).thenReturn(true);
        when(saveMethod.getParameterList()).thenReturn(emptyParams);

        when(mockClass.getAllMethods()).thenReturn(new PsiMethod[]{saveMethod});
        when(mockClass.getAllFields()).thenReturn(new PsiField[0]);

        List<PsiElement> targets = WOComponentClassResolver.findSingleKeyTargets(mockClass, "save");
        assertFalse(targets.isEmpty());
        assertEquals(saveMethod, targets.get(0));
    }

    @Test
    public void testFindSingleKeyTargetsFieldAndBackingField() {
        PsiClass mockClass = mock(PsiClass.class);
        when(mockClass.getAllMethods()).thenReturn(new PsiMethod[0]);

        PsiField titleField = mock(PsiField.class);
        when(titleField.getName()).thenReturn("title");

        PsiField backingField = mock(PsiField.class);
        when(backingField.getName()).thenReturn("_itemCount");

        when(mockClass.getAllFields()).thenReturn(new PsiField[]{titleField, backingField});

        List<PsiElement> targetsTitle = WOComponentClassResolver.findSingleKeyTargets(mockClass, "title");
        assertFalse(targetsTitle.isEmpty());
        assertEquals(titleField, targetsTitle.get(0));

        List<PsiElement> targetsCount = WOComponentClassResolver.findSingleKeyTargets(mockClass, "itemCount");
        assertFalse(targetsCount.isEmpty());
        assertEquals(backingField, targetsCount.get(0));
    }

    @Test
    public void testCollectBindingVariants() {
        PsiClass mockClass = mock(PsiClass.class);

        // Getter: String getName()
        PsiMethod getNameMethod = mock(PsiMethod.class);
        when(getNameMethod.getName()).thenReturn("getName");
        PsiParameterList emptyParams = mock(PsiParameterList.class);
        when(emptyParams.isEmpty()).thenReturn(true);
        when(getNameMethod.getParameterList()).thenReturn(emptyParams);
        when(getNameMethod.getReturnType()).thenReturn(null);

        // Action: WOActionResults performAction()
        PsiMethod actionMethod = mock(PsiMethod.class);
        when(actionMethod.getName()).thenReturn("performAction");
        when(actionMethod.getParameterList()).thenReturn(emptyParams);
        when(actionMethod.getReturnType()).thenReturn(null);

        // Field: int counter
        PsiField counterField = mock(PsiField.class);
        when(counterField.getName()).thenReturn("counter");
        when(counterField.getType()).thenReturn(PsiTypes.intType());

        when(mockClass.getAllMethods()).thenReturn(new PsiMethod[]{getNameMethod, actionMethod});
        when(mockClass.getAllFields()).thenReturn(new PsiField[]{counterField});

        List<LookupElement> variants = WOComponentClassResolver.collectBindingVariants(mockClass);
        assertEquals(3, variants.size());

        assertTrue(variants.stream().anyMatch(v -> v.getLookupString().equals("name")));
        assertTrue(variants.stream().anyMatch(v -> v.getLookupString().equals("performAction")));
        assertTrue(variants.stream().anyMatch(v -> v.getLookupString().equals("counter")));
    }

    @Test
    public void testReferenceRangeExtractionSingleBinding() {
        List<WOHtmlBindingReferenceContributor.BindingSegment> segments =
                WOHtmlBindingReferenceContributor.parseBindingSegments("\"$title\"");
        assertEquals(1, segments.size());
        assertEquals(new com.intellij.openapi.util.TextRange(1, 7), segments.get(0).range());
        assertEquals("title", segments.get(0).fullPath());
        assertEquals("title", segments.get(0).key());

        // Single quoted
        List<WOHtmlBindingReferenceContributor.BindingSegment> singleQuoted =
                WOHtmlBindingReferenceContributor.parseBindingSegments("'$saveAction'");
        assertEquals(1, singleQuoted.size());
        assertEquals(new com.intellij.openapi.util.TextRange(1, 12), singleQuoted.get(0).range());
        assertEquals("saveAction", singleQuoted.get(0).fullPath());

        // Unquoted
        List<WOHtmlBindingReferenceContributor.BindingSegment> unquoted =
                WOHtmlBindingReferenceContributor.parseBindingSegments("$myField");
        assertEquals(1, unquoted.size());
        assertEquals(new com.intellij.openapi.util.TextRange(0, 8), unquoted.get(0).range());
        assertEquals("myField", unquoted.get(0).fullPath());
    }

    @Test
    public void testReferenceRangeExtractionKeypath() {
        List<WOHtmlBindingReferenceContributor.BindingSegment> segments =
                WOHtmlBindingReferenceContributor.parseBindingSegments("\"$user.address.street\"");
        assertEquals(3, segments.size());

        // Segment 0: "$user" in [1, 6]
        assertEquals(new com.intellij.openapi.util.TextRange(1, 6), segments.get(0).range());
        assertEquals("user", segments.get(0).fullPath());
        assertEquals("user", segments.get(0).key());

        // Segment 1: "address" in [7, 14]
        assertEquals(new com.intellij.openapi.util.TextRange(7, 14), segments.get(1).range());
        assertEquals("user.address", segments.get(1).fullPath());
        assertEquals("address", segments.get(1).key());

        // Segment 2: "street" in [15, 21]
        assertEquals(new com.intellij.openapi.util.TextRange(15, 21), segments.get(2).range());
        assertEquals("user.address.street", segments.get(2).fullPath());
        assertEquals("street", segments.get(2).key());
    }

    @Test
    public void testReferenceRangeExtractionWithMethodParens() {
        // e.g. "$doAction()"
        List<WOHtmlBindingReferenceContributor.BindingSegment> segments =
                WOHtmlBindingReferenceContributor.parseBindingSegments("\"$doAction()\"");
        assertEquals(1, segments.size());
        assertEquals(new com.intellij.openapi.util.TextRange(1, 12), segments.get(0).range());
        assertEquals("doAction", segments.get(0).fullPath());
        assertEquals("doAction", segments.get(0).key());
    }

    @Test
    public void testXmlAttributeDescriptorRequiredAndTypeText() {
        PsiElement mockDecl = mock(PsiElement.class);
        WOXmlAttributeDescriptor normal = new WOXmlAttributeDescriptor("optionalBinding", mockDecl);
        assertFalse(normal.isRequired());
        assertNull(normal.getTypeText());
        assertNull(normal.getRequiredMessage());

        WOXmlAttributeDescriptor required = new WOXmlAttributeDescriptor(
                "stageName",
                mockDecl,
                true,
                "Boolean",
                "'stageName' is a required binding."
        );
        assertTrue(required.isRequired());
        assertEquals("Boolean", required.getTypeText());
        assertEquals("'stageName' is a required binding.", required.getRequiredMessage());
        assertEquals("stageName", required.getName());
    }
}
