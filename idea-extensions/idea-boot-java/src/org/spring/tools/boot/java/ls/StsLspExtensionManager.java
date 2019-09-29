package org.spring.tools.boot.java.ls;

import com.intellij.ide.ApplicationActivationStateManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.spring.tools.boot.java.ls.extensions.StsLabelProvider;
import org.wso2.lsp4intellij.client.ClientContext;
import org.wso2.lsp4intellij.client.languageserver.ServerOptions;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.DefaultRequestManager;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.wso2.lsp4intellij.client.languageserver.wrapper.LanguageServerWrapper;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.extensions.LSPExtensionManager;
import org.wso2.lsp4intellij.extensions.LSPLabelProvider;
import org.wso2.lsp4intellij.listeners.EditorMouseMotionListenerImpl;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

import static org.spring.tools.boot.java.ls.ApplicationUtils.runReadAction;

public class StsLspExtensionManager implements LSPExtensionManager {

    private static final Predicate<? super VirtualFile> SPRING_PREDICATE =
            (f) -> f.getPath().contains("spring-core") || f.getPath().contains("spring-boot");

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

    @Override
    public boolean isFileContentSupported(@NotNull PsiFile file) {
        return runReadAction(() ->
                Optional.ofNullable(FileIndexFacade.getInstance(file.getProject()).getModuleForFile(file.getVirtualFile()))
                        .map(this::isSpringModule).orElse(false));
    }

    private boolean isSpringModule(Module module) {
        return Arrays.stream(ModuleRootManager.getInstance(module).orderEntries().librariesOnly().classes().getRoots())
                .anyMatch(SPRING_PREDICATE);
    }

    @NotNull
    @Override
    public LSPLabelProvider getLabelProvider() {
        return new StsLabelProvider();
    }
}
