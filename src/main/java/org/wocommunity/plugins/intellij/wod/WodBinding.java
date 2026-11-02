package org.wocommunity.plugins.intellij.wod;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WodBinding {
    private final String key;
    private final String value;
    private final TextRange keyRange;
    private final TextRange valueRange;
    private final boolean isLiteralString;

    public WodBinding(@NotNull String key,
                      @NotNull String value,
                      @Nullable TextRange keyRange,
                      @Nullable TextRange valueRange,
                      boolean isLiteralString) {
        this.key = key;
        this.value = value;
        this.keyRange = keyRange;
        this.valueRange = valueRange;
        this.isLiteralString = isLiteralString;
    }

    public @NotNull String getKey() {
        return key;
    }

    public @NotNull String getValue() {
        return value;
    }

    public @Nullable TextRange getKeyRange() {
        return keyRange;
    }

    public @Nullable TextRange getValueRange() {
        return valueRange;
    }

    public boolean isLiteralString() {
        return isLiteralString;
    }

    @Override
    public String toString() {
        return key + " = " + value + ";";
    }
}
