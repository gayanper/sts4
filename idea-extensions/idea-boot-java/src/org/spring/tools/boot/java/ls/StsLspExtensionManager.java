package org.spring.tools.boot.java.ls;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorMouseListener;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.wso2.lsp4intellij.client.ClientContext;
import org.wso2.lsp4intellij.client.languageserver.ServerOptions;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.DefaultRequestManager;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.extensions.LSPExtensionManager;
import org.wso2.lsp4intellij.listeners.EditorMouseMotionListenerImpl;

public class StsLspExtensionManager implements LSPExtensionManager {

    @Override
    public <T extends DefaultRequestManager> T getExtendedRequestManagerFor(
        LanguageServerWrapper languageServerWrapper, LanguageServer languageServer,
        LanguageClient languageClient, ServerCapabilities serverCapabilities) {
        return (T) new DefaultRequestManager(languageServerWrapper, languageServer, languageClient,
            serverCapabilities);
    }

    @Override
    public <T extends EditorEventManager> T getExtendedEditorEventManagerFor(Editor editor,
                                                                             DocumentListener documentListener, EditorMouseListener editorMouseListener,
                                                                             EditorMouseMotionListenerImpl editorMouseMotionListener, RequestManager requestManager,
                                                                             ServerOptions serverOptions, LanguageServerWrapper languageServerWrapper) {
        return (T) new EditorEventManager(editor, documentListener, editorMouseListener,
            editorMouseMotionListener, requestManager, serverOptions, languageServerWrapper);
    }

    @Override
    public Class<? extends LanguageServer> getExtendedServerInterface() {
        return LanguageServer.class;
    }

    @Override
    public LanguageClient getExtendedClientFor(ClientContext clientContext) {
        return new StsLanuageClient(clientContext);
    }
}
