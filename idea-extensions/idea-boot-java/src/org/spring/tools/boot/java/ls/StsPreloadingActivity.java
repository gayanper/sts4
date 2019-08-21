package org.spring.tools.boot.java.ls;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.requests.Timeouts;

public class StsPreloadingActivity extends PreloadingActivity {

    private static final Logger LOGGER = Logger.getInstance(StsPreloadingActivity.class);
    private static final String PTRN_JAVA = "java";
    public static final String LANG_ID_JAVA = PTRN_JAVA;
    public static final String LANG_ID_XML = "xml";
    public static final String LANG_ID_PROPERTIES = "spring-boot-properties";
    public static final String LANG_ID_YAML = "spring-boot-properties-yaml";
    private static final String PTRN_APPLICATION_YAML = "application.*\\.yaml";
    private static final String PTRN_APPLICATION_YML = "application.*\\.yml";
    private static final String PTRN_CONTEXT_XML = ".*[Cc]ontext.*\\.xml";
    private static final String PTRN_APPLICATION_PROPERTIES = "application.*\\.properties";

    @Override
    public void preload(@NotNull ProgressIndicator progressIndicator) {
        if (Strings.isNullOrEmpty(System.getProperty(PTRN_JAVA + ".home"))) {
            LOGGER.error("No " + PTRN_JAVA + "home found in system properties");
            return;
        }

        if (Prerequisities.isBelowJava8()) {
            LOGGER.error("Unsupported " + PTRN_JAVA + " version, 1.8 or above is required");
            return;
        }

        IntellijLanguageClient.setTimeout(Timeouts.INIT, 60000);
        IntellijLanguageClient.setTimeout(Timeouts.COMPLETION, 5000);

        IntellijLanguageClient.addServerDefinition(
                StsServiceDefinitionBuilder.forExtensions(String.format("%s,%s,%s,%s,%s", PTRN_JAVA, PTRN_APPLICATION_YAML, PTRN_APPLICATION_YML, PTRN_CONTEXT_XML, PTRN_APPLICATION_PROPERTIES))
                        .withLanguageMapping(PTRN_JAVA, LANG_ID_JAVA)
                        .withLanguageMapping("yaml", LANG_ID_YAML)
                        .withLanguageMapping("yml", LANG_ID_YAML)
                        .withLanguageMapping("xml", LANG_ID_XML)
                        .withLanguageMapping("properties", LANG_ID_PROPERTIES)
                        .withServerListener().build());

        StsLspExtensionManager extensionManager = new StsLspExtensionManager();
        Lists.newArrayList(PTRN_JAVA, PTRN_APPLICATION_YAML, PTRN_APPLICATION_YML, PTRN_CONTEXT_XML, PTRN_APPLICATION_PROPERTIES)
                .forEach(e -> IntellijLanguageClient.addExtensionManager(e, extensionManager));
    }
}
