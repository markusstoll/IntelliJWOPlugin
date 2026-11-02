package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.psi.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WOApplicationClassUtilTest {

    @Test
    public void testIsWOApplicationClassDirect() {
        PsiClass directWO = mock(PsiClass.class);
        when(directWO.getQualifiedName()).thenReturn("com.webobjects.appserver.WOApplication");
        assertTrue(WOApplicationClassUtil.isWOApplicationClass(directWO));

        PsiClass directERX = mock(PsiClass.class);
        when(directERX.getQualifiedName()).thenReturn("er.extensions.appserver.ERXApplication");
        assertTrue(WOApplicationClassUtil.isWOApplicationClass(directERX));
    }

    @Test
    public void testIsWOApplicationClassViaSuperClass() {
        PsiClass appClass = mock(PsiClass.class);
        when(appClass.getQualifiedName()).thenReturn("com.example.Application");

        PsiClass erxClass = mock(PsiClass.class);
        when(erxClass.getQualifiedName()).thenReturn("er.extensions.appserver.ERXApplication");
        when(appClass.getSuperClass()).thenReturn(erxClass);

        assertTrue(WOApplicationClassUtil.isWOApplicationClass(appClass));
    }

    @Test
    public void testIsWOApplicationClassRejectsInterfacesAndUnrelated() {
        assertFalse(WOApplicationClassUtil.isWOApplicationClass(null));

        PsiClass iface = mock(PsiClass.class);
        when(iface.isInterface()).thenReturn(true);
        when(iface.getQualifiedName()).thenReturn("com.example.SomeInterface");
        assertFalse(WOApplicationClassUtil.isWOApplicationClass(iface));

        PsiClass unrelated = mock(PsiClass.class);
        when(unrelated.getQualifiedName()).thenReturn("com.example.Unrelated");
        PsiClass objectClass = mock(PsiClass.class);
        when(objectClass.getQualifiedName()).thenReturn("java.lang.Object");
        when(unrelated.getSuperClass()).thenReturn(objectClass);
        assertFalse(WOApplicationClassUtil.isWOApplicationClass(unrelated));
    }

    @Test
    public void testHasExecutableMainMethodInCompanionObject() {
        PsiClass appClass = mock(PsiClass.class);
        when(appClass.getQualifiedName()).thenReturn("com.example.KotlinApp");
        when(appClass.getMethods()).thenReturn(new PsiMethod[0]);
        PsiClass erxClass = mock(PsiClass.class);
        when(erxClass.getQualifiedName()).thenReturn("er.extensions.appserver.ERXApplication");
        when(appClass.getSuperClass()).thenReturn(erxClass);

        PsiClass companion = mock(PsiClass.class);
        when(companion.getName()).thenReturn("Companion");
        PsiMethod mainMethod = mock(PsiMethod.class);
        when(mainMethod.getName()).thenReturn("main");
        when(companion.getMethods()).thenReturn(new PsiMethod[]{mainMethod});

        when(appClass.getInnerClasses()).thenReturn(new PsiClass[]{companion});
        assertTrue(WOApplicationClassUtil.hasExecutableMainMethod(appClass));
        assertTrue(WOApplicationClassUtil.hasCompanionMainWithoutJvmStatic(appClass));
        assertTrue(WOApplicationClassUtil.isCandidateMainClass(appClass));
    }

    @Test
    public void testCandidateMainClassForKotlinFileWithTopLevelMain() {
        // Simulates Application.kt containing:
        // class Application : ERXApplication
        // fun main(args: Array<String>) -> compiled as ApplicationKt with main
        PsiClassOwner ktFile = mock(PsiClassOwner.class);

        PsiClass appClass = mock(PsiClass.class);
        when(appClass.getName()).thenReturn("Application");
        when(appClass.getQualifiedName()).thenReturn("com.example.Application");
        when(appClass.getContainingFile()).thenReturn(ktFile);
        when(appClass.getMethods()).thenReturn(new PsiMethod[0]);
        PsiClass erxClass = mock(PsiClass.class);
        when(erxClass.getQualifiedName()).thenReturn("er.extensions.appserver.ERXApplication");
        when(appClass.getSuperClass()).thenReturn(erxClass);
        when(appClass.getInnerClasses()).thenReturn(new PsiClass[0]);

        PsiClass facadeClass = mock(PsiClass.class);
        when(facadeClass.getName()).thenReturn("ApplicationKt");
        when(facadeClass.getQualifiedName()).thenReturn("com.example.ApplicationKt");
        when(facadeClass.getContainingFile()).thenReturn(ktFile);
        when(facadeClass.getInnerClasses()).thenReturn(new PsiClass[0]);
        PsiMethod facadeMain = mock(PsiMethod.class);
        when(facadeMain.getName()).thenReturn("main");
        PsiParameterList paramList = mock(PsiParameterList.class);
        when(paramList.getParametersCount()).thenReturn(1);
        when(facadeMain.getParameterList()).thenReturn(paramList);
        PsiModifierList modifierList = mock(PsiModifierList.class);
        when(modifierList.hasModifierProperty(PsiModifier.STATIC)).thenReturn(true);
        when(modifierList.hasModifierProperty(PsiModifier.PUBLIC)).thenReturn(true);
        when(facadeMain.getModifierList()).thenReturn(modifierList);
        when(facadeClass.findMethodsByName("main", true)).thenReturn(new PsiMethod[]{facadeMain});
        when(facadeClass.getMethods()).thenReturn(new PsiMethod[]{facadeMain});

        when(ktFile.getClasses()).thenReturn(new PsiClass[]{appClass, facadeClass});

        // 1. Application class in file should be accepted (because file has executable main)
        assertTrue(WOApplicationClassUtil.fileHasMainMethod(appClass));
        assertTrue(WOApplicationClassUtil.isCandidateMainClass(appClass));

        // 2. ApplicationKt facade class:
        assertTrue(WOApplicationClassUtil.isAssociatedWithWOApplication(facadeClass));
        // Always hidden from chooser because it is a synthetic Kotlin facade (*Kt)
        assertFalse(WOApplicationClassUtil.isCandidateMainClass(facadeClass));
    }
}
