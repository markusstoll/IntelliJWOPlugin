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
 * Loads WebObjects system element definitions from bundled {@code /wotemplate/WebObjectDefinitions.xml}.
 *
 * Provides binding lists for standard dynamic elements such as {@code WOConditional}, {@code WOString},
 * {@code WOGenericContainer}, etc.
 */
public final class WOSystemBindingDefinitions {

    private static final String RESOURCE_PATH = "/wotemplate/WebObjectDefinitions.xml";

    public static class Binding {
        public final String name;
        public final boolean required;

        public Binding(String name, boolean required) {
            this.name = name;
            this.required = required;
        }

        public String getName() {
            return name;
        }

        public boolean isRequired() {
            return required;
        }
    }

    private static final Map<String, Set<Binding>> BINDINGS_BY_SHORT_CLASS_NAME = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> BINDING_NAMES_BY_SHORT_CLASS_NAME = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> REQUIRED_BINDINGS_BY_SHORT_CLASS_NAME = new ConcurrentHashMap<>();
    private static volatile boolean loaded;

    private WOSystemBindingDefinitions() {
    }

    public static @NotNull Set<Binding> getBindingsForShortClassName(@NotNull String shortClassName) {
        ensureLoaded();
        Set<Binding> b = BINDINGS_BY_SHORT_CLASS_NAME.get(shortClassName);
        return b != null ? b : Collections.emptySet();
    }

    public static @NotNull Set<String> getBindingNamesForShortClassName(@NotNull String shortClassName) {
        ensureLoaded();
        Set<String> b = BINDING_NAMES_BY_SHORT_CLASS_NAME.get(shortClassName);
        return b != null ? b : Collections.emptySet();
    }

    public static @NotNull Set<String> getRequiredBindingsForShortClassName(@NotNull String shortClassName) {
        ensureLoaded();
        Set<String> b = REQUIRED_BINDINGS_BY_SHORT_CLASS_NAME.get(shortClassName);
        return b != null ? b : Collections.emptySet();
    }

    public static @NotNull Set<String> getAllShortClassNames() {
        ensureLoaded();
        return Collections.unmodifiableSet(BINDINGS_BY_SHORT_CLASS_NAME.keySet());
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (WOSystemBindingDefinitions.class) {
            if (loaded) {
                return;
            }
            loadDefinitions();
            loaded = true;
        }
    }

    private static void loadDefinitions() {
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

            var elements = root.getElementsByTagName("wo");
            for (int i = 0; i < elements.getLength(); i++) {
                var node = elements.item(i);
                if (!(node instanceof org.w3c.dom.Element e)) {
                    continue;
                }

                String cls = e.getAttribute("class");
                if (cls == null || cls.isBlank()) {
                    continue;
                }
                String shortName = cls.trim();

                Set<Binding> bindings = new LinkedHashSet<>();
                Set<String> bindingNames = new LinkedHashSet<>();
                Set<String> requiredBindings = new LinkedHashSet<>();
                var bindingNodes = e.getElementsByTagName("binding");
                for (int j = 0; j < bindingNodes.getLength(); j++) {
                    var bn = bindingNodes.item(j);
                    if (!(bn instanceof org.w3c.dom.Element be)) {
                        continue;
                    }
                    String name = be.getAttribute("name");
                    if (name != null && !name.isBlank()) {
                        String clean = name.trim();
                        String req = be.getAttribute("required");
                        boolean isReq = "YES".equalsIgnoreCase(req) || "true".equalsIgnoreCase(req);
                        bindings.add(new Binding(clean, isReq));
                        bindingNames.add(clean);
                        if (isReq) {
                            requiredBindings.add(clean);
                        }
                    }
                }

                if (!bindings.isEmpty()) {
                    BINDINGS_BY_SHORT_CLASS_NAME.put(shortName, Collections.unmodifiableSet(bindings));
                    BINDING_NAMES_BY_SHORT_CLASS_NAME.put(shortName, Collections.unmodifiableSet(bindingNames));
                }
                if (!requiredBindings.isEmpty()) {
                    REQUIRED_BINDINGS_BY_SHORT_CLASS_NAME.put(shortName, Collections.unmodifiableSet(requiredBindings));
                }
            }
        } catch (Exception ignored) {
            // Keep definitions empty on parse/IO issues.
        }
    }
}
