package org.wocommunity.plugins.intellij;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaCommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import org.jetbrains.annotations.NotNull;

public class WOApplicationCommandLineState<T extends WOApplicationConfiguration> extends ApplicationConfiguration.JavaApplicationCommandLineState<T> {
    public WOApplicationCommandLineState(T configuration, @NotNull ExecutionEnvironment environment) {
        super(configuration, environment);
    }

    @Override
    protected JavaParameters createJavaParameters() throws ExecutionException {
        JavaParameters javaParameters = super.createJavaParameters();

        ParametersList vmParametersList = javaParameters.getVMParametersList();

        Sdk jdk = javaParameters.getJdk();
        SdkTypeId sdkType = jdk.getSdkType();
        JavaSdk javaSdk = (JavaSdk) sdkType;
        if(javaSdk.isOfVersionOrHigher(jdk, JavaSdkVersion.JDK_11))
        {
            vmParametersList.add("--add-exports=java.base/sun.security.action=ALL-UNNAMED");
            vmParametersList.add("--add-exports=java.base/sun.util.calendar=ALL-UNNAMED");
            vmParametersList.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
        }
        vmParametersList.add("-DNSProjectBundleEnabled=true");
        vmParametersList.add("-DNSProjectSearchPath=(..,..)");

        ParametersList programParametersList = javaParameters.getProgramParametersList();

        programParametersList.add("-NSOpenProjectIDE", "WOLips");
        programParametersList.add("-WOAcceptMalformedCookies", "false");
        programParametersList.add("-WOAdaptor", "WODefaultAdaptor");
        programParametersList.add("-WOAdaptorURL", "http://127.0.0.1/cgi-bin/WebObjects");
        programParametersList.add("-WOAdditionalAdaptors", "()");
        programParametersList.add("-WOAllowRapidTurnaround", "true");
        programParametersList.add("-WOAllowsCacheControlHeader", "true");
        programParametersList.add("-WOAllowsConcurrentRequestHandling", "false");
        programParametersList.add("-WOApplicationBaseURL", "/WebObjects");
        programParametersList.add("-WOAutoOpenClientApplication", "true");
        programParametersList.add("-WOAutoOpenInBrowser", "true");
        programParametersList.add("-WOCachingEnabled", "false");
        programParametersList.add("-WOContextClassName", "WOContext");
        programParametersList.add("-WODebuggingEnabled", "true");
        programParametersList.add("-WODefaultUndoStackLimit", "10");
        programParametersList.add("-WODirectConnectEnabled", "true");
        programParametersList.add("-WODisplayExceptionPages", "true");
        programParametersList.add("-WOFrameworksBaseURL", "/WebObjects/Frameworks");
        programParametersList.add("-WOGenerateWSDL", "true");
        programParametersList.add("-WOHost", "127.0.0.1");
        programParametersList.add("-WOIDE", "WOLips");
        programParametersList.add("-WOIncludeCommentsInResponse", "false");
        programParametersList.add("-WOLifebeatDestinationPort", "1085");
        programParametersList.add("-WOLifebeatEnabled", "true");
        programParametersList.add("-WOLifebeatInterval", "30");
        programParametersList.add("-WOListenQueueSize", "128");
        programParametersList.add("-WOMaxHeaders", "200");
        programParametersList.add("-WOMaxIOBufferSize", "8196");
        programParametersList.add("-WOMaxSocketIdleTime", "180000");
        programParametersList.add("-WOMissingResourceSearchEnabled", "true");
        programParametersList.add("-WOMonitorEnabled", "false");
        programParametersList.add("-WOPort", "-1");
        programParametersList.add("-WOSMTPHost", "smtp");
        programParametersList.add("-WOSessionStoreClassName", "WOServerSessionStore");
        programParametersList.add("-WOSessionTimeOut", "3600");
        programParametersList.add("-WOSocketCacheSize", "100");
        programParametersList.add("-WOSocketMonitorSleepTime", "50");
        programParametersList.add("-WOWorkerThreadCount", "8");
        programParametersList.add("-WOWorkerThreadCountMax", "256");
        programParametersList.add("-WOWorkerThreadCountMin", "16");

        return javaParameters;
    }
}
