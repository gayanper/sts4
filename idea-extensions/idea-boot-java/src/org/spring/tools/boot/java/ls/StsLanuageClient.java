package org.spring.tools.boot.java.ls;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.MarkupContent;
import org.spring.tools.boot.java.ls.highlight.HighlightProcessor;
import org.spring.tools.boot.java.ls.highlight.InlayHighlightProcessor;
import org.spring.tools.boot.java.ls.highlight.RangeHighlightProcessor;
import org.spring.tools.boot.java.ls.java.ClasspathListener;
import org.spring.tools.boot.java.ls.java.TypeDescriptorProvider;
import org.spring.tools.boot.java.ls.java.TypeProvider;
import org.springframework.ide.vscode.commons.protocol.CursorMovement;
import org.springframework.ide.vscode.commons.protocol.HighlightParams;
import org.springframework.ide.vscode.commons.protocol.ProgressParams;
import org.springframework.ide.vscode.commons.protocol.STS4LanguageClient;
import org.springframework.ide.vscode.commons.protocol.java.*;
import org.wso2.lsp4intellij.client.ClientContext;
import org.wso2.lsp4intellij.client.DefaultLanguageClient;
import org.wso2.lsp4intellij.editor.EditorEventManager;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;

class StsLanuageClient extends DefaultLanguageClient implements STS4LanguageClient {

    private static final Logger LOGGER = Logger.getInstance(StsLanuageClient.class);

    private List<HighlightProcessor> processors;

    private Map<String, ClasspathListener> classpathListenerMap = new HashMap<>();

    private TypeProvider typeProvider;

    private TypeDescriptorProvider typeDescriptorProvider;

    public StsLanuageClient(ClientContext clientContext) {
        super(clientContext);
        processors = ImmutableList.of(new RangeHighlightProcessor(), new InlayHighlightProcessor());
        typeProvider = new TypeProvider(clientContext.getProject());
        typeDescriptorProvider = new TypeDescriptorProvider();
    }
    private void processHighlights(HighlightParams params, String documentUri, Editor editor,
                                   Document document) {
        processors.forEach(p -> p.preProcess(documentUri, editor));
        params.getCodeLenses()
                .forEach(l -> processors.forEach(p -> p.process(documentUri, l, editor)));
    }


    @Override
    public void highlight(HighlightParams params) {
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

    @Override
    public void progress(ProgressParams progressEvent) {

    }

    @Override
    public CompletableFuture<Object> moveCursor(CursorMovement cursorMovement) {
        return null;
    }

    @Override
    public CompletableFuture<Object> addClasspathListener(ClasspathListenerParams params) {
        ClasspathListener classpathListener = ClasspathListener.from(params, getContext().getProject());
        classpathListenerMap.put(params.getCallbackCommandId(), classpathListener);
        //classpathListener.register(getContext().getRequestManager());
        return CompletableFuture.completedFuture(new Object());
    }

    @Override
    public CompletableFuture<Object> removeClasspathListener(ClasspathListenerParams params) {
        ClasspathListener classpathListener = classpathListenerMap.remove(params.getCallbackCommandId());
        if (classpathListener != null) {
            classpathListener.unregister();
        } else {
            LOGGER.warn("removeClasspathListener was called for unregistered listener [callbackId:"
                    + params.getCallbackCommandId() + "]");
        }
        return CompletableFuture.completedFuture(new Object());
    }

    @Override
    public CompletableFuture<MarkupContent> javadoc(org.springframework.ide.vscode.commons.protocol.java.JavaDataParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<TypeData> javaType(JavaDataParams params) {
        return CompletableFuture.completedFuture(typeProvider.typeDataFor(params.getBindingKey()));
    }

    @Override
    public CompletableFuture<String> javadocHoverLink(org.springframework.ide.vscode.commons.protocol.java.JavaDataParams params) {
        return CompletableFuture.completedFuture("");
    }

    @Override
    public CompletableFuture<Location> javaLocation(org.springframework.ide.vscode.commons.protocol.java.JavaDataParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<TypeDescriptorData>> javaSearchTypes(JavaSearchParams params) {
        return CompletableFuture.completedFuture(typeDescriptorProvider.descriptors(PsiShortNamesCache.getInstance(getContext().getProject())
                .getClassesByName(params.getTerm(), GlobalSearchScope.allScope(getContext().getProject()))));
    }

    @Override
    public CompletableFuture<List<String>> javaSearchPackages(JavaSearchParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<List<TypeDescriptorData>> javaSubTypes(JavaTypeHierarchyParams params) {
        return CompletableFuture.completedFuture(findClass(params).map(clazz -> {
            List<PsiClass> subtypes = Lists.newCopyOnWriteArrayList(ClassInheritorsSearch.search(clazz, true).findAll());
            if(params.isIncludeFocusType()) {
                subtypes.add(clazz);
            }
            return typeDescriptorProvider.descriptors(subtypes.toArray(new PsiClass[0]));
        }).orElse(Collections.emptyList()));
    }

    @Override
    public CompletableFuture<List<TypeDescriptorData>> javaSuperTypes(JavaTypeHierarchyParams params) {
        return CompletableFuture.completedFuture(findClass(params).map(clazz -> {
            return typeDescriptorProvider.descriptors(clazz.getSupers());
        }).orElse(Collections.emptyList()));
    }

    @Override
    public CompletableFuture<List<JavaCodeCompleteData>> javaCodeComplete(JavaCodeCompleteParams params) {
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    private Optional<PsiClass> findClass(JavaTypeHierarchyParams params) {
        return Optional.ofNullable(JavaPsiFacade.getInstance(getContext().getProject()).findClass(params.getFqName(),
                GlobalSearchScope.allScope(getContext().getProject())));
    }

}
