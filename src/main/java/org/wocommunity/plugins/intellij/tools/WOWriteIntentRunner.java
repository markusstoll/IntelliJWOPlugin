package org.wocommunity.plugins.intellij.tools;

import com.intellij.openapi.util.Computable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utility to run operations requiring Write-Intent lock on the EDT (such as creating embedded editors)
 * without generating static bytecode references to the experimental WriteIntentReadAction API.
 */
public final class WOWriteIntentRunner {

    private static final Method COMPUTE_METHOD;

    static {
        Method m = null;
        try {
            Class<?> clazz = Class.forName("com.intellij.openapi.application.WriteIntentReadAction");
            m = clazz.getMethod("compute", Computable.class);
        } catch (Throwable ignored) {
        }
        COMPUTE_METHOD = m;
    }

    private WOWriteIntentRunner() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T compute(Computable<T> computable) {
        if (COMPUTE_METHOD != null) {
            try {
                return (T) COMPUTE_METHOD.invoke(null, computable);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException re) {
                    throw re;
                }
                if (cause instanceof Error err) {
                    throw err;
                }
                throw new RuntimeException(cause != null ? cause : e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return computable.compute();
    }
}

