package org.wocommunity.plugins.intellij.tools;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;

public class WOComponentFileCreator {
    public static void createWOComponent(Module module,
                                         String componentName,
                                         VirtualFile componentsFolder,
                                         boolean createHtmlContents,
                                         boolean createApiFile) throws IOException {
        @NotNull VirtualFile woComponentFolder = componentsFolder.createChildDirectory(null, componentName + ".wo");

        if(createApiFile)
        {
            String apiFileTemplate = IOUtils.toString(WOProjectUtil.class.getResource("/templates/WOComponent.api"), "UTF-8");
            apiFileTemplate = apiFileTemplate.replace("{name}", componentName);

            @NotNull VirtualFile apiFile = componentsFolder.createChildData(null, componentName + ".api");

            try (OutputStream outputStream = apiFile.getOutputStream(null)) {
                outputStream.write(apiFileTemplate.getBytes());
            }
        }

        @NotNull VirtualFile htmlFile = woComponentFolder.createChildData(null, componentName + ".html");
        if(createHtmlContents) {
            String htmlFileTemplate = IOUtils.toString(WOProjectUtil.class.getResource("/templates/WOComponent.html"), "UTF-8");

            try (OutputStream outputStream = htmlFile.getOutputStream(null)) {
                outputStream.write(htmlFileTemplate.getBytes());
            }
        }

        @NotNull VirtualFile wodFile = woComponentFolder.createChildData(null, componentName + ".wod");
        @NotNull VirtualFile wooFile = woComponentFolder.createChildData(null, componentName + ".woo");
        {
            String wooFileTemplate = IOUtils.toString(WOProjectUtil.class.getResource("/templates/WOComponent.woo"), "UTF-8");

            try (OutputStream outputStream = wooFile.getOutputStream(null)) {
                outputStream.write(wooFileTemplate.getBytes());
            }
        }

    }
}
