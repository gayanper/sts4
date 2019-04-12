package org.spring.tools.boot.java.ls;

import org.wso2.lsp4intellij.client.LanguageClientImpl;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.RawCommandServerDefinition;

public class StsServerDefinition extends RawCommandServerDefinition {

    public StsServerDefinition(String ext, String id, String[] command) {
        super(ext, id, command);
    }

    @Override
    public LanguageClientImpl createLanguageClient() {
        return super.createLanguageClient();
    }
}
