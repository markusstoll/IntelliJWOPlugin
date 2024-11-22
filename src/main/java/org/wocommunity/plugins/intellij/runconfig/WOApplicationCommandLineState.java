package org.wocommunity.plugins.intellij.runconfig;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.wocommunity.plugins.intellij.tools.WOProjectUtil;
import org.wocommunity.plugins.intellij.runconfig.data.KeyValueOption;

import java.io.File;

public class WOApplicationCommandLineState<T extends WOApplicationConfiguration> extends ApplicationConfiguration.JavaApplicationCommandLineState<T> {
    public WOApplicationCommandLineState(T configuration, @NotNull ExecutionEnvironment environment) {
        super(configuration, environment);
    }

    @Override
    protected JavaParameters createJavaParameters() throws ExecutionException {
        JavaParameters javaParameters = super.createJavaParameters();

        String modulePath = WOProjectUtil.getModulePath(myConfiguration.getConfigurationModule().getModule());

        if(StringUtils.isEmpty(javaParameters.getWorkingDirectory())
            || javaParameters.getWorkingDirectory().equals(myConfiguration.getProject().getBasePath()))
        {
            javaParameters.setWorkingDirectory(modulePath + "/target/" + myConfiguration.getConfigurationModule().getModule().getName() + ".woa");
        }

        ParametersList vmParametersList = javaParameters.getVMParametersList();

        Sdk jdk = javaParameters.getJdk();
        SdkTypeId sdkType = jdk.getSdkType();
        JavaSdk javaSdk = (JavaSdk) sdkType;
        if(javaSdk.isOfVersionOrHigher(jdk, JavaSdkVersion.JDK_11))
        {
            for(String vmParameter : myConfiguration.getOptions().getHigherJdkVMParameters()) {
                vmParametersList.add(vmParameter);
            }
        }

        ParametersList programParametersList = javaParameters.getProgramParametersList();

        for(KeyValueOption kvo : myConfiguration.getOptions().getWoOptions()) {
            if(!kvo.getActive())
                continue;

            if (kvo.key.startsWith("-D"))
            {
                vmParametersList.add(kvo.key + "=" + kvo.value);
            } else
            {
                programParametersList.add(kvo.key, kvo.value);
            }
        }

        new WOProjectUtil().createOrUpdateProjectDescriptionFile(new File(modulePath));

        return javaParameters;
    }
}
