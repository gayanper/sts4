package org.spring.tools.boot.java.ls;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import java.util.List;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.spring.tools.boot.java.ls.highlight.HighlightParams;
import org.spring.tools.boot.java.ls.highlight.HighlightProcessor;
import org.spring.tools.boot.java.ls.highlight.InlayHighlightProcessor;
import org.spring.tools.boot.java.ls.highlight.RangeHighlightProcessor;
import org.wso2.lsp4intellij.client.ClientContext;
import org.wso2.lsp4intellij.client.DefaultLanguageClient;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.utils.FileUtils;

class StsLanuageClient extends DefaultLanguageClient {

    private static final Logger LOGGER = Logger.getInstance(StsLanuageClient.class);

    private List<HighlightProcessor> processors;

    public StsLanuageClient(ClientContext clientContext) {
        super(clientContext);
        processors = ImmutableList.of(new RangeHighlightProcessor(), new InlayHighlightProcessor());
    }

    @JsonNotification("sts/highlight")
    public void stsHighlight(HighlightParams params) {
        LOGGER.debug("Processing highligh notification for document uri :",
            params.getDoc().getUri());

        final String documentUri = FileUtils.sanitizeURI(params.getDoc().getUri());
        final EditorEventManager editorEventManager = getContext()
            .getEditorEventManagerFor(documentUri);

        if (editorEventManager == null || editorEventManager.editor == null
            || editorEventManager.editor.getDocument() == null) {
            LOGGER.debug("Editor is not initialized for processing highlights");
            return;
        }

        final Document document = editorEventManager.editor.getDocument();
        ApplicationManager.getApplication()
            .invokeLater(
                () -> processHighlights(params, documentUri, editorEventManager.editor, document));
    }

    private void processHighlights(HighlightParams params, String documentUri, Editor editor,
        Document document) {
        processors.forEach(p -> p.preProcess(documentUri, editor));
        params.getCodeLenses()
            .forEach(l -> processors.forEach(p -> p.process(documentUri, l, editor)));
    }
}
