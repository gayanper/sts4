package org.spring.tools.boot.java.ls;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.spring.tools.boot.java.ls.highlight.HighlightParams;
import org.spring.tools.boot.java.ls.highlight.HighlightProcessor;
import org.spring.tools.boot.java.ls.highlight.InlayHighlightProcessor;
import org.spring.tools.boot.java.ls.highlight.RangeHighlightProcessor;
import org.spring.tools.boot.java.ls.java.ClasspathListener;
import org.spring.tools.boot.java.ls.java.ClasspathListenerParams;
import org.spring.tools.boot.java.ls.java.JavaDataParams;
import org.wso2.lsp4intellij.client.ClientContext;
import org.wso2.lsp4intellij.client.DefaultLanguageClient;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StsLanuageClient extends DefaultLanguageClient {

    private static final Logger LOGGER = Logger.getInstance(StsLanuageClient.class);

    private List<HighlightProcessor> processors;

    private Map<String, ClasspathListener> classpathListenerMap = new HashMap<>();

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

    @JsonNotification("sts/addClasspathListener")
    public void addClasspathListener(ClasspathListenerParams params) {
        ClasspathListener classpathListener = ClasspathListener.from(params, getContext().getProject());
        classpathListenerMap.put(params.getCallbackCommandId(), classpathListener);
        classpathListener.register(getContext().getRequestManager());
    }

    @JsonNotification("sts/removeClasspathListener")
    public void removeClasspathListener(ClasspathListenerParams params) {
        ClasspathListener classpathListener = classpathListenerMap.remove(params.getCallbackCommandId());
        if (classpathListener != null) {
            classpathListener.unregister();
        } else {
            LOGGER.warn("removeClasspathListener was called for unregistered listener [callbackId:"
                    + params.getCallbackCommandId() + "]");
        }
    }

    @JsonNotification("sts/javaType")
    public void javaType(JavaDataParams params) {

    }
}
