package org.wocommunity.plugins.intellij.wizards;

import org.jetbrains.annotations.NotNull;
import java.io.File;

public class WOProjectModel {
    private String name = "MyWonderApp";
    private String location = "";
    private String groupId = "your.domain";
    private String artifactId = "MyWonderApp";
    private String version = "1.0.0-SNAPSHOT";
    private String packageName = "your.domain.app";
    private WOArchetype archetype = WOArchetype.ERX_APPLICATION;
    private String archetypeVersion = WOArchetype.DEFAULT_ARCHETYPE_VERSION;
    private String javaVersion = "17";
    private String wonderVersion = "7.2";
    private boolean wonderSupport = true;
    private boolean addRepositoryToPom = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public WOArchetype getArchetype() {
        return archetype;
    }

    public void setArchetype(WOArchetype archetype) {
        this.archetype = archetype;
    }

    public String getArchetypeVersion() {
        return archetypeVersion;
    }

    public void setArchetypeVersion(String archetypeVersion) {
        this.archetypeVersion = archetypeVersion;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getWonderVersion() {
        return wonderVersion;
    }

    public void setWonderVersion(String wonderVersion) {
        this.wonderVersion = wonderVersion;
    }

    public boolean isWonderSupport() {
        return wonderSupport;
    }

    public void setWonderSupport(boolean wonderSupport) {
        this.wonderSupport = wonderSupport;
    }

    public boolean isAddRepositoryToPom() {
        return addRepositoryToPom;
    }

    public void setAddRepositoryToPom(boolean addRepositoryToPom) {
        this.addRepositoryToPom = addRepositoryToPom;
    }

    public @NotNull String getEffectiveLocation() {
        if (location == null || location.isBlank()) {
            return "";
        }
        File loc = new File(location);
        if (!loc.getName().equalsIgnoreCase(artifactId)) {
            return new File(loc, artifactId).getAbsolutePath();
        }
        return loc.getAbsolutePath();
    }
}

