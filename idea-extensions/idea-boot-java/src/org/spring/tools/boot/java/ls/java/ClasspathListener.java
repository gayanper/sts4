package org.spring.tools.boot.java.ls.java;

import com.google.common.collect.Lists;
import com.intellij.ProjectTopics;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.jetbrains.annotations.NotNull;
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

import static org.spring.tools.boot.java.ls.ApplicationUtils.runReadAction;
import static org.spring.tools.boot.java.ls.java.CommonMappings.fromFirst;
import static org.spring.tools.boot.java.ls.java.CommonMappings.toBinaryCPE;

public class ClasspathListener {
    private static final Logger LOGGER = Logger.getInstance(ClasspathListener.class);

    private String callbackCommandId;
    private Project project;
    private RequestManager requestManager;
    private MessageBusConnection messageBusConnection;

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
        messageBusConnection.subscribe(ProjectTopics.PROJECT_ROOTS, new LSModuleRootListener());
        sendClasspathCommand(runReadAction(this::collectCPEs), false);
    }

    public void unregister() {
        messageBusConnection.disconnect();
        messageBusConnection = null;
        requestManager = null;
    }

    private List<org.springframework.ide.vscode.commons.protocol.java.Classpath.CPE> collectCPEs() {
        final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
        final List<org.springframework.ide.vscode.commons.protocol.java.Classpath.CPE> cpes = new ArrayList<>();

        Arrays.stream(ModuleManager.getInstance(project).getModules()).parallel().forEach(m -> {
            final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(m);
            final String outputUrl = fromFirst(moduleRootManager.orderEntries().withoutLibraries().withoutDepModules()
                    .withoutModuleSourceEntries().withoutSdk().classes().getRoots(), f -> f.getUrl()).orElse("");
            Arrays.stream(moduleRootManager.getSourceRoots(true))
                    .map(f -> mapSourceRoot(f, outputUrl)).forEach(cpes::add);
        });

        projectRootManager.orderEntries().forEachLibrary(l -> {
            String sourcePath = fromFirst(l.getFiles(OrderRootType.SOURCES), f -> f.getUrl()).orElse("");
            String javadocPath = fromFirst(l.getFiles(OrderRootType.DOCUMENTATION), f -> f.getUrl()).orElse("");

            Arrays.stream(l.getFiles(OrderRootType.CLASSES)).map(f -> {
                CPE cpe = toBinaryCPE(f);
                try {
                    cpe.setJavadocContainerUrl(new File(javadocPath).toURL());
                    cpe.setSourceContainerUrl(new File(sourcePath).toURL());
                } catch (MalformedURLException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                return cpe;
            }).forEach(cpes::add);
            return true;
        });
        return cpes;
    }

    private CPE mapSourceRoot(VirtualFile file, String outputUrl) {
        return CPE.source(new File(file.getPath()), new File(outputUrl));
    }

    private void sendClasspathCommand(Collection<CPE> entries, boolean deleted) {
        ExecuteCommandParams commandParams = new ExecuteCommandParams();
        commandParams.setCommand(callbackCommandId);

        Classpath classpath = new Classpath(Lists.newArrayList(entries));
        commandParams.setArguments(ClasspathArgument.argument(project.getName())
                .projectUri(FileUtils.projectToUri(project)).classpath(classpath).arguments());
        CompletableFuture<Object> result = requestManager.executeCommand(commandParams);
        try {
            if (!"done".equals(result.get())) {
                LOGGER.error("executeCommand failed for callback " + callbackCommandId
                        + " with error code:" + result.get().toString());
            }
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error(e);
        }
    }

    private class LSModuleRootListener implements ModuleRootListener {
        private List<CPE> before = Collections.emptyList();

        @Override
        public void rootsChanged(@NotNull ModuleRootEvent event) {
            List<CPE> after = collectCPEs();
            Collection<CPE> removed = CollectionUtils.subtract(before, after);

            sendClasspathCommand(removed, true);
            sendClasspathCommand(after, false);
        }

        @Override
        public void beforeRootsChange(@NotNull ModuleRootEvent event) {
            before = collectCPEs();
        }
    }
}
