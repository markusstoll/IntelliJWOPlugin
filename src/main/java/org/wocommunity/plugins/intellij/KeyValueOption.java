package org.wocommunity.plugins.intellij;

public class KeyValueOption {
    private boolean isActive;
    private String key;
    private String value;

    public KeyValueOption(boolean isActive, String key, String value) {
        this.isActive = isActive;
        this.key = key;
        this.value = value;
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}

