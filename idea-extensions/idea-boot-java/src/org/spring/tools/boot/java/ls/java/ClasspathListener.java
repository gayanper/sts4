package org.spring.tools.boot.java.ls.java;

import com.google.common.collect.Lists;
import com.intellij.ProjectTopics;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.springframework.ide.vscode.commons.protocol.java.ClasspathListenerParams;
import org.wso2.lsp4intellij.client.languageserver.requestmanager.RequestManager;
import org.springframework.ide.vscode.commons.protocol.java.Classpath.CPE;
import org.springframework.ide.vscode.commons.protocol.java.Classpath;
import org.wso2.lsp4intellij.utils.FileUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.spring.tools.boot.java.ls.ApplicationUtils.runReadAction;
import static org.spring.tools.boot.java.ls.java.CommonUtils.fromFirst;
import static org.spring.tools.boot.java.ls.java.CommonUtils.toBinaryCPE;

public class ClasspathListener {
    private static final Logger LOGGER = Logger.getInstance(ClasspathListener.class);

    private String callbackCommandId;
    private Project project;
    private RequestManager requestManager;
    private MessageBusConnection messageBusConnection;
    private LSModuleRootListener moduleRootListener;

    private Object requestManagerSync = new Object();

    private ClasspathListener(String callbackCommandId, Project project) {
        this.callbackCommandId = callbackCommandId;
        this.project = project;
    }

    public static ClasspathListener from(ClasspathListenerParams params, Project project) {
        return new ClasspathListener(params.getCallbackCommandId(), project);
    }

    public void register(RequestManager requestManager) {
        this.requestManager = requestManager;
        messageBusConnection = project.getMessageBus().connect(project);
        moduleRootListener = new LSModuleRootListener();
        messageBusConnection.subscribe(ProjectTopics.PROJECT_ROOTS, moduleRootListener);
        List<CPE> list = runReadAction(this::collectCPEs);
        sendClasspathCommand(list, false);
        moduleRootListener.updateBefore(list);
    }

    public void unregister() {
        messageBusConnection.disconnect();
        messageBusConnection = null;
        synchronized (requestManagerSync) {
            requestManager = null;
        }
    }

    private List<org.springframework.ide.vscode.commons.protocol.java.Classpath.CPE> collectCPEs() {
        final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
        final List<org.springframework.ide.vscode.commons.protocol.java.Classpath.CPE> cpes = new ArrayList<>();

        Arrays.stream(ModuleManager.getInstance(project).getModules()).parallel().forEach(m -> {
            final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(m);
            final String outputUrl = CommonUtils.outputDir(m);
            final String testOutputUrl = CommonUtils.testOutputDir(m);

            moduleRootManager.getSourceRoots(JavaSourceRootType.SOURCE).stream()
                    .map(f -> mapSourceRoot(f, outputUrl, true, false)).forEach(cpes::add);
            moduleRootManager.getSourceRoots(JavaResourceRootType.RESOURCE).stream()
                    .map(f -> mapSourceRoot(f, outputUrl, false, false)).forEach(cpes::add);

            moduleRootManager.getSourceRoots(JavaSourceRootType.TEST_SOURCE).stream()
                    .map(f -> mapSourceRoot(f, testOutputUrl, true, true)).forEach(cpes::add);
            moduleRootManager.getSourceRoots(JavaResourceRootType.TEST_RESOURCE).stream()
                    .map(f -> mapSourceRoot(f, testOutputUrl, false, true)).forEach(cpes::add);
        });

        projectRootManager.orderEntries().withoutSdk().forEachLibrary(l -> {
            processLibrary(cpes, false, l::getFiles);
            return true;
        });
        processLibrary(cpes, true, projectRootManager.getProjectSdk().getRootProvider()::getFiles);
        return cpes;
    }

    private void processLibrary(List<CPE> cpes, boolean sdk, Function<OrderRootType, VirtualFile[]> files) {
        String sourcePath = fromFirst(files.apply(OrderRootType.SOURCES), f -> f.getUrl()).orElse("");
        String javadocPath = fromFirst(files.apply(OrderRootType.DOCUMENTATION), f -> f.getUrl()).orElse("");

        Arrays.stream(files.apply(OrderRootType.CLASSES)).map(f -> {
            CPE cpe = toBinaryCPE(f);
            try {
                cpe.setJavadocContainerUrl(new File(javadocPath).toURL());
                cpe.setSourceContainerUrl(new File(sourcePath).toURL());
            } catch (MalformedURLException e) {
                LOGGER.error(e.getMessage(), e);
            }
            cpe.setOwn(false);
            cpe.setSystem(sdk);
            cpe.setJavaContent(true);
            return cpe;
        }).forEach(cpes::add);
    }

    private CPE mapSourceRoot(VirtualFile file, String outputUrl, boolean isJava, boolean isTest) {
        CPE cpe = CPE.source(new File(file.getPath()), new File(outputUrl));
        cpe.setOwn(true);
        cpe.setTest(isTest);
        cpe.setSystem(false);
        cpe.setJavaContent(isJava);
        return cpe;
    }

    private void sendClasspathCommand(Collection<CPE> entries, boolean deleted) {
        ExecuteCommandParams commandParams = new ExecuteCommandParams();
        commandParams.setCommand(callbackCommandId);

        Classpath classpath = new Classpath(Lists.newArrayList(entries));
        commandParams.setArguments(ClasspathArgument.argument(project.getName())
                .projectUri(FileUtils.projectToUri(project)).classpath(classpath).arguments());

        synchronized (requestManagerSync) {
            if(requestManager != null) {
                CompletableFuture<Object> result =
                       Optional.ofNullable(requestManager.executeCommand(commandParams))
                               .orElse(CompletableFuture.completedFuture("stopped"));
                try {
                    if (!"stopped".equals(result.get()) && !"done".equals(result.get())) {
                        LOGGER.error("executeCommand failed for callback " + callbackCommandId
                                + " with error code:" + result.get().toString());
                    }
                } catch (ExecutionException | InterruptedException e) {
                    LOGGER.error(e);
                }
            }
        }
    }

    private class LSModuleRootListener implements ModuleRootListener {
        private List<CPE> before = Collections.emptyList();

        @Override
        public void rootsChanged(@NotNull ModuleRootEvent event) {
            CompletableFuture.supplyAsync(() -> {
                List<CPE> after = runReadAction(ClasspathListener.this::collectCPEs);
                Collection<CPE> removed = CollectionUtils.subtract(before, after);

                sendClasspathCommand(removed, true);
                sendClasspathCommand(after, false);
                return after;
            }).thenAccept(this::updateBefore);
        }

        public void updateBefore(List<CPE> before) {
            this.before = before;
        }
    }
}
