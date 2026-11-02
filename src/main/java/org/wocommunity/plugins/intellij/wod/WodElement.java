package org.wocommunity.plugins.intellij.wod;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class WodElement {
    private final String name;
    private final String typeName;
    private final TextRange nameRange;
    private final TextRange typeRange;
    private final TextRange totalRange;
    private final Map<String, WodBinding> bindings = new LinkedHashMap<>();

    public WodElement(@NotNull String name,
                      @NotNull String typeName,
                      @Nullable TextRange nameRange,
                      @Nullable TextRange typeRange,
                      @Nullable TextRange totalRange) {
        this.name = name;
        this.typeName = typeName;
        this.nameRange = nameRange;
        this.typeRange = typeRange;
        this.totalRange = totalRange;
    }

    public void addBinding(@NotNull WodBinding binding) {
        bindings.put(binding.getKey(), binding);
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String getTypeName() {
        return typeName;
    }

    public @Nullable TextRange getNameRange() {
        return nameRange;
    }

    public @Nullable TextRange getTypeRange() {
        return typeRange;
    }

    public @Nullable TextRange getTotalRange() {
        return totalRange;
    }

    public @NotNull Map<String, WodBinding> getBindings() {
        return Collections.unmodifiableMap(bindings);
    }

    public @Nullable WodBinding getBinding(@NotNull String key) {
        return bindings.get(key);
    }

    public boolean hasBinding(@NotNull String key) {
        return bindings.containsKey(key);
    }

    @Override
    public String toString() {
        return name + " : " + typeName + " { ... }";
    }
}
