package org.wocommunity.plugins.intellij.wizards;

import org.jetbrains.annotations.NotNull;

public enum WOArchetype {
    ERX_APPLICATION(
            "erxapplication-archetype",
            "Wonder Application",
            "Full Project Wonder application with ERXApplication, Main component, Session, and DirectAction.",
            true,
            true
    ),
    ERX_APPLICATION_D2W(
            "erxapplication-d2w-archetype",
            "Wonder D2W Application",
            "Project Wonder Direct to Web (D2W) application with Rule System and ERD2W components.",
            true,
            true
    ),
    ERX_APPLICATION_SERVLET(
            "erxapplication-servlet-archetype",
            "Wonder Servlet Application",
            "Project Wonder application configured for deployment as a standard Java Servlet / WAR.",
            true,
            true
    ),
    WO_APPLICATION(
            "woapplication-archetype",
            "WebObjects Application (Plain)",
            "Standard WebObjects application without Project Wonder dependencies.",
            false,
            true
    ),
    WO_FRAMEWORK(
            "woframework-archetype",
            "WebObjects Framework",
            "WebObjects / Wonder reusable framework project with optional Wonder support.",
            true,
            false
    ),
    WO_APPLICATION_SERVLET(
            "woapplication-servlet-archetype",
            "WebObjects Servlet Application (Plain)",
            "Standard WebObjects servlet application without Project Wonder dependencies.",
            false,
            true
    ),
    PARTIAL_WO_SERVLET(
            "partial-woapplication-servlet-archetype",
            "Partial WebObjects Servlet Application",
            "Partial WebObjects servlet application project archetype.",
            false,
            true
    );

    public static final String GROUP_ID = "org.wocommunity";
    public static final String REPOSITORY_URL = "https://maven.wocommunity.org/content/groups/public/";
    public static final String DEFAULT_ARCHETYPE_VERSION = "3.0";

    private final String artifactId;
    private final String displayName;
    private final String description;
    private final boolean wonder;
    private final boolean application;

    WOArchetype(String artifactId, String displayName, String description, boolean wonder, boolean application) {
        this.artifactId = artifactId;
        this.displayName = displayName;
        this.description = description;
        this.wonder = wonder;
        this.application = application;
    }

    public @NotNull String getArtifactId() {
        return artifactId;
    }

    public @NotNull String getDisplayName() {
        return displayName;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public boolean isWonder() {
        return wonder;
    }

    public boolean isApplication() {
        return application;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

