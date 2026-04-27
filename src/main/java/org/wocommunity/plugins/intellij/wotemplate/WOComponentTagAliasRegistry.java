package org.wocommunity.plugins.intellij.wotemplate;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

final class WOComponentTagAliasRegistry {

    private static final String RESOURCE_PATH = "/wotemplate/wo-tag-aliases.properties";
    private static final Map<String, String> ALIASES = new ConcurrentHashMap<>();
    private static volatile boolean loaded;

    private WOComponentTagAliasRegistry() {
    }

    static @NotNull String resolveAlias(@NotNull String rawLocalName) {
        ensureLoaded();
        String mapped = ALIASES.get(rawLocalName);
        return mapped != null ? mapped : rawLocalName;
    }

    static @NotNull Map<String, String> getAliases() {
        ensureLoaded();
        return Collections.unmodifiableMap(ALIASES);
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (WOComponentTagAliasRegistry.class) {
            if (loaded) {
                return;
            }
            loadFromResource();
            loaded = true;
        }
    }

    private static void loadFromResource() {
        try (InputStream in = WOComponentTagAliasRegistry.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                return;
            }
            Properties props = new Properties();
            props.load(in);
            for (Map.Entry<Object, Object> e : props.entrySet()) {
                String k = String.valueOf(e.getKey()).trim();
                String v = String.valueOf(e.getValue()).trim();
                if (!k.isEmpty() && !v.isEmpty()) {
                    ALIASES.put(k, v);
                }
            }
        } catch (IOException ignored) {
            // keep aliases empty on IO issues
        }
    }
}

