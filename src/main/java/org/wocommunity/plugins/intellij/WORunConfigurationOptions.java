package org.wocommunity.plugins.intellij;

import com.intellij.execution.application.JvmMainMethodRunConfigurationOptions;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.openapi.components.StoredProperty;
import com.intellij.openapi.components.StoredPropertyBase;
import com.intellij.openapi.observable.properties.PropertyGraph;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;

public class WORunConfigurationOptions extends JvmMainMethodRunConfigurationOptions {

    @OptionTag("woOptions")
    // A custom stored property to track changes in the list of options
    private StoredProperty<List<KeyValueOption>> woOptionsProperty = list();

    // Getter for the list of custom options
    public List<KeyValueOption> getWoOptions() {
        return woOptionsProperty.getValue(this);
    }

    public void setWoOptions(List<KeyValueOption> woOptions) {
        this.woOptionsProperty.setValue(this, woOptions);
    }

    public WORunConfigurationOptions() {
        woOptionsProperty.setName("woOptions");

        List<KeyValueOption> defaultsOptions = new ArrayList<>();
        defaultsOptions.add(new KeyValueOption(true, "-DNSProjectBundleEnabled", "true"));
        defaultsOptions.add(new KeyValueOption(true, "-DNSProjectSearchPath", "Automatic"));
        defaultsOptions.add(new KeyValueOption(true, "-NSOpenProjectIDE", "WOLips"));
        defaultsOptions.add(new KeyValueOption(true, "-WOAcceptMalformedCookies", "false"));
        defaultsOptions.add(new KeyValueOption(true, "-WOAdaptor", "WODefaultAdaptor"));
        defaultsOptions.add(new KeyValueOption(true, "-WOAdaptorURL", "http://127.0.0.1/cgi-bin/WebObjects"));
        defaultsOptions.add(new KeyValueOption(true, "-WOAdditionalAdaptors", "()"));
        defaultsOptions.add(new KeyValueOption(true, "-WOAllowRapidTurnaround", "true"));
        defaultsOptions.add(new KeyValueOption(true, "-WOAllowsCacheControlHeader", "true"));
        defaultsOptions.add(new KeyValueOption(true, "-WOAllowsConcurrentRequestHandling", "false"));
        defaultsOptions.add(new KeyValueOption(true, "-WOApplicationBaseURL", "/WebObjects"));
        defaultsOptions.add(new KeyValueOption(true, "-WOAutoOpenClientApplication", "true"));
        defaultsOptions.add(new KeyValueOption(true, "-WOAutoOpenInBrowser", "true"));
        defaultsOptions.add(new KeyValueOption(true, "-WOCachingEnabled", "false"));
        defaultsOptions.add(new KeyValueOption(true, "-WOContextClassName", "WOContext"));
        defaultsOptions.add(new KeyValueOption(true, "-WODebuggingEnabled", "true"));
        defaultsOptions.add(new KeyValueOption(true, "-WODefaultUndoStackLimit", "10"));
        defaultsOptions.add(new KeyValueOption(true, "-WODirectConnectEnabled", "true"));
        defaultsOptions.add(new KeyValueOption(true, "-WODisplayExceptionPages", "true"));
        defaultsOptions.add(new KeyValueOption(true, "-WOFrameworksBaseURL", "/WebObjects/Frameworks"));
        defaultsOptions.add(new KeyValueOption(true, "-WOGenerateWSDL", "true"));
        defaultsOptions.add(new KeyValueOption(true, "-WOHost", "127.0.0.1"));
        defaultsOptions.add(new KeyValueOption(true, "-WOIDE", "WOLips"));
        defaultsOptions.add(new KeyValueOption(true, "-WOIncludeCommentsInResponse", "false"));
        defaultsOptions.add(new KeyValueOption(true, "-WOLifebeatDestinationPort", "1085"));
        defaultsOptions.add(new KeyValueOption(true, "-WOLifebeatEnabled", "true"));
        defaultsOptions.add(new KeyValueOption(true, "-WOLifebeatInterval", "30"));
        defaultsOptions.add(new KeyValueOption(true, "-WOListenQueueSize", "128"));
        defaultsOptions.add(new KeyValueOption(true, "-WOMaxHeaders", "200"));
        defaultsOptions.add(new KeyValueOption(true, "-WOMaxIOBufferSize", "8196"));
        defaultsOptions.add(new KeyValueOption(true, "-WOMaxSocketIdleTime", "180000"));
        defaultsOptions.add(new KeyValueOption(true, "-WOMissingResourceSearchEnabled", "true"));
        defaultsOptions.add(new KeyValueOption(true, "-WOMonitorEnabled", "false"));
        defaultsOptions.add(new KeyValueOption(true, "-WOPort", "-1"));
        defaultsOptions.add(new KeyValueOption(true, "-WOSMTPHost", "smtp"));
        defaultsOptions.add(new KeyValueOption(true, "-WOSessionStoreClassName", "WOServerSessionStore"));
        defaultsOptions.add(new KeyValueOption(true, "-WOSessionTimeOut", "3600"));
        defaultsOptions.add(new KeyValueOption(true, "-WOSocketCacheSize", "100"));
        defaultsOptions.add(new KeyValueOption(true, "-WOSocketMonitorSleepTime", "50"));
        defaultsOptions.add(new KeyValueOption(true, "-WOWorkerThreadCount", "8"));
        defaultsOptions.add(new KeyValueOption(true, "-WOWorkerThreadCountMax", "256"));
        defaultsOptions.add(new KeyValueOption(true, "-WOWorkerThreadCountMin", "16"));

        setWoOptions(defaultsOptions);
    }
}
