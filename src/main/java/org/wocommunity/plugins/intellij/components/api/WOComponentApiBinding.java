package org.wocommunity.plugins.intellij.components.api;

import java.util.Objects;

/**
 * Represents a single binding definition in a WebObjects Component API file (.api).
 */
public class WOComponentApiBinding {
    private String name;
    private String defaults;
    private boolean required;
    private boolean willSet;
    private String requiredMessage;

    public WOComponentApiBinding() {
        this("", null, false, false);
    }

    public WOComponentApiBinding(String name) {
        this(name, null, false, false);
    }

    public WOComponentApiBinding(String name, String defaults, boolean required, boolean willSet) {
        this(name, defaults, required, willSet, null);
    }

    public WOComponentApiBinding(String name, String defaults, boolean required, boolean willSet, String requiredMessage) {
        this.name = name != null ? name : "";
        this.defaults = (defaults != null && !defaults.isBlank()) ? defaults : null;
        this.required = required;
        this.willSet = willSet;
        this.requiredMessage = requiredMessage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getDefaults() {
        return defaults;
    }

    public void setDefaults(String defaults) {
        this.defaults = (defaults != null && !defaults.isBlank()) ? defaults : null;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isWillSet() {
        return willSet;
    }

    public void setWillSet(boolean willSet) {
        this.willSet = willSet;
    }

    public String getRequiredMessage() {
        return requiredMessage;
    }

    public void setRequiredMessage(String requiredMessage) {
        this.requiredMessage = requiredMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WOComponentApiBinding that = (WOComponentApiBinding) o;
        return required == that.required &&
                willSet == that.willSet &&
                Objects.equals(name, that.name) &&
                Objects.equals(defaults, that.defaults);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, defaults, required, willSet);
    }

    @Override
    public String toString() {
        return name;
    }
}

