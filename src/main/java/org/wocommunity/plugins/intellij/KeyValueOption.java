package org.wocommunity.plugins.intellij;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;

import java.util.Objects;

@Tag("KeyValueOption")
public class KeyValueOption {
    @Attribute
    public boolean active;
    @Attribute
    public String key;
    @Attribute
    public String value;

    public KeyValueOption(boolean isActive, String key, String value) {
        this.active = isActive;
        this.key = key;
        this.value = value;
    }

    public KeyValueOption()
    {
    }

    public boolean getActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public int hashCode() {
        return Objects.hash(new Object[]{active, key, value});
    }

    @Override
    public boolean equals(Object obj) {
        return hashCode() == obj.hashCode();
    }

    @Override
    public KeyValueOption clone() {
        return new KeyValueOption(active, key, value);
    }
}

