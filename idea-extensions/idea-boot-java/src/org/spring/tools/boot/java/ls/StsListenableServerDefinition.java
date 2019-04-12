package org.spring.tools.boot.java.ls;

import org.wso2.lsp4intellij.client.LanguageClientImpl;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.RawCommandServerDefinition;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.ServerListener;

public class StsListenableServerDefinition extends RawCommandServerDefinition {

    public StsListenableServerDefinition(String ext, String id, String[] command) {
        super(ext, id, command);
    }

    @Override
    public LanguageClientImpl createLanguageClient() {
        return new StsLanuageClient();
    }

    @Override
    public ServerListener getServerListener() {
        return new StsServerListener();
    }
}
