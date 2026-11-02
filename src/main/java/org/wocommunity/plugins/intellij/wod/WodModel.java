package org.wocommunity.plugins.intellij.wod;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class WodModel {
    private final Map<String, WodElement> elements = new LinkedHashMap<>();

    public void addElement(@NotNull WodElement element) {
        elements.put(element.getName(), element);
    }

    public @Nullable WodElement getElement(@NotNull String name) {
        return elements.get(name);
    }

    public boolean hasElement(@NotNull String name) {
        return elements.containsKey(name);
    }

    public @NotNull Collection<WodElement> getElements() {
        return Collections.unmodifiableCollection(elements.values());
    }

    public @NotNull Map<String, WodElement> getElementMap() {
        return Collections.unmodifiableMap(elements);
    }
}
