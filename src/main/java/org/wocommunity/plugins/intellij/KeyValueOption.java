package org.wocommunity.plugins.intellij;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;

@Tag("KeyValueOption")
public class KeyValueOption {
    @Attribute
    public boolean isActive;
    @Attribute
    public String key;
    @Attribute
    public String value;

    public KeyValueOption(boolean isActive, String key, String value) {
        this.isActive = isActive;
        this.key = key;
        this.value = value;
    }

    public KeyValueOption()
    {
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}

