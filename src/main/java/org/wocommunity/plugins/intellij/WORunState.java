package org.wocommunity.plugins.intellij;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.application.BaseJavaApplicationCommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.util.JavaParametersUtil;

public class WORunState extends BaseJavaApplicationCommandLineState<WOApplicationConfiguration> {
    public WORunState(ExecutionEnvironment environment, WOApplicationConfiguration configuration) {
        super(environment, configuration);
    }

    @Override
    protected JavaParameters createJavaParameters() throws ExecutionException {
        final JavaParameters params = new JavaParameters();
        final JavaRunConfigurationModule module = myConfiguration.getConfigurationModule();
        final String jreHome = myConfiguration.isAlternativeJrePathEnabled() ? myConfiguration.getAlternativeJrePath() : null;
        final int classPathType = JavaParametersUtil.getClasspathType(module,
  //              WORunConfig.MAIN_CLASS_NAME,
                null,
                false);

        JavaParametersUtil.configureModule(module, params, classPathType, jreHome);
        params.setJdk(JavaParametersUtil.createProjectJdk(myConfiguration.getProject(), jreHome));
//        params.setMainClass(WORunConfig.MAIN_CLASS_NAME);

//        EvaluationConfig evaluationConfig = EvaluationConfig.from(myConfiguration);
//        evaluationConfig.populateParameterList(params.getProgramParametersList());

//        params.getClassPath().add(PathUtil.getJarPathForClass(RunPageObjectMain.class));
//        params.getClassPath().add(PathUtil.getJarPathForClass(Logger.class));
        setupJavaParameters(params);

        return params;
    }
}
