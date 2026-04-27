package org.wocommunity.plugins.intellij.wotemplate;

import org.jetbrains.annotations.NotNull;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads WO system component bindings from {@code /wotemplate/WebObjectDefinitions.xml}.
 *
 * The file contains class names without package, e.g. {@code WOGenericElement}.
 */
final class WOSystemBindingDefinitions {

    private static final String RESOURCE_PATH = "/wotemplate/WebObjectDefinitions.xml";

    private static final Map<String, Set<String>> BINDINGS_BY_SHORT_CLASS_NAME = new ConcurrentHashMap<>();
    private static volatile boolean loaded;

    private WOSystemBindingDefinitions() {
    }

    static @NotNull Set<String> getBindingsForShortClassName(@NotNull String shortClassName) {
        ensureLoaded();
        Set<String> b = BINDINGS_BY_SHORT_CLASS_NAME.get(shortClassName);
        return b != null ? b : Collections.emptySet();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (WOSystemBindingDefinitions.class) {
            if (loaded) {
                return;
            }
            loadFromResource();
            loaded = true;
        }
    }

    private static void loadFromResource() {
        try (InputStream in = WOSystemBindingDefinitions.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                return;
            }

            var dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            try {
                dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
                dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            } catch (Exception ignored) {
                // best-effort hardening
            }

            var doc = dbf.newDocumentBuilder().parse(in);
            var root = doc.getDocumentElement();
            if (root == null) {
                return;
            }

            var woNodes = root.getElementsByTagName("wo");
            for (int i = 0; i < woNodes.getLength(); i++) {
                var wo = woNodes.item(i);
                if (!(wo instanceof org.w3c.dom.Element e)) {
                    continue;
                }

                String cls = e.getAttribute("class");
                if (cls == null || cls.isBlank()) {
                    continue;
                }
                String shortName = cls.trim();

                Set<String> bindings = new LinkedHashSet<>();
                var bindingNodes = e.getElementsByTagName("binding");
                for (int j = 0; j < bindingNodes.getLength(); j++) {
                    var bn = bindingNodes.item(j);
                    if (!(bn instanceof org.w3c.dom.Element be)) {
                        continue;
                    }
                    String name = be.getAttribute("name");
                    if (name != null && !name.isBlank()) {
                        bindings.add(name.trim());
                    }
                }

                if (!bindings.isEmpty()) {
                    BINDINGS_BY_SHORT_CLASS_NAME.put(shortName, Collections.unmodifiableSet(bindings));
                }
            }
        } catch (Exception ignored) {
            // Keep definitions empty on parse/IO issues.
        }
    }
}

