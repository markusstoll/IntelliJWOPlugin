package org.wocommunity.plugins.intellij.components.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing the contents of a WebObjects Component API file (.api).
 */
public class WOComponentApiModel {
    private String componentClassName;
    private boolean componentContent;
    private final List<WOComponentApiBinding> bindings;
    private final List<String> extraValidationsXml;

    public WOComponentApiModel() {
        this("", false);
    }

    public WOComponentApiModel(String componentClassName, boolean componentContent) {
        this.componentClassName = componentClassName != null ? componentClassName : "";
        this.componentContent = componentContent;
        this.bindings = new ArrayList<>();
        this.extraValidationsXml = new ArrayList<>();
    }

    public String getComponentClassName() {
        return componentClassName;
    }

    public void setComponentClassName(String componentClassName) {
        this.componentClassName = componentClassName != null ? componentClassName : "";
    }

    public boolean isComponentContent() {
        return componentContent;
    }

    public void setComponentContent(boolean componentContent) {
        this.componentContent = componentContent;
    }

    public List<WOComponentApiBinding> getBindings() {
        return bindings;
    }

    public void addBinding(WOComponentApiBinding binding) {
        if (binding != null) {
            bindings.add(binding);
        }
    }

    public boolean removeBinding(WOComponentApiBinding binding) {
        return bindings.remove(binding);
    }

    public List<String> getExtraValidationsXml() {
        return extraValidationsXml;
    }
}

