package org.spring.tools.boot.java.ls;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.diagnostic.Logger;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.ServerListener;

class StsServerListener implements ServerListener {

    private static final Logger LOGGER = Logger.getInstance(StsServerListener.class);

    @Override
    public void initialize(@NotNull LanguageServer server, @NotNull InitializeResult result) {
        ImmutableMap<String, ImmutableMap<String, ImmutableMap<String, String>>> configJsonObject = ImmutableMap
            .of("boot-java",
                ImmutableMap.of("support-spring-xml-config", ImmutableMap.of("on", "true",
                        "hyperlinks", "true", "scan-folders-globs", "**/src/main/**")));
        server.getWorkspaceService()
            .didChangeConfiguration(new DidChangeConfigurationParams(configJsonObject));
    }
}
