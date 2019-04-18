package org.spring.tools.boot.java.ls;

import com.google.common.base.Strings;
import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;

public class StsPreloadingActivity extends PreloadingActivity {

    private static final Logger LOGGER = Logger.getInstance(StsPreloadingActivity.class);
    public static final String LANG_ID_JAVA = "java";
    public static final String LANG_ID_PROPERTIES = "spring-boot-properties";
    public static final String LANG_ID_YAML = "spring-boot-properties-yaml";

    @Override
    public void preload(@NotNull ProgressIndicator progressIndicator) {
        if (Strings.isNullOrEmpty(System.getProperty("java.home"))) {
            LOGGER.error("No javahome found in system properties");
            return;
        }

        if (Prerequisities.isBelowJava8()) {
            LOGGER.error("Unsupported java version, 1.8 or above is required");
            return;
        }

        IntellijLanguageClient.addServerDefinition(
            StsServiceDefinitionBuilder.forLanguage(LANG_ID_JAVA).withExtension("java")
                .withServerListener().build());
        IntellijLanguageClient.addExtensionManager("java", new StsLspExtensionManager());

        IntellijLanguageClient.addServerDefinition(
            StsServiceDefinitionBuilder.forLanguage(LANG_ID_PROPERTIES)
                .withExtension("application*.properties").build());

        IntellijLanguageClient.addServerDefinition(
            StsServiceDefinitionBuilder.forLanguage(LANG_ID_PROPERTIES)
                .withExtension("application.*\\.properties").build());

        IntellijLanguageClient.addServerDefinition(
            StsServiceDefinitionBuilder.forLanguage(LANG_ID_YAML)
                .withExtension("application.*\\.yaml,application.*\\.yml").build());
    }
}
