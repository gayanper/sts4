package org.spring.tools.boot.java.ls;

import com.intellij.openapi.diagnostic.Logger;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.RawCommandServerDefinition;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public final class StsServiceDefinitionBuilder {

    private static final Logger LOGGER = Logger.getInstance(StsServiceDefinitionBuilder.class);
    public static final String LAUNCHER = "org.springframework.boot.loader.JarLauncher";

    private String extensions;
    private String langId;
    private boolean serverListenerEnabled = false;
    private Map<String, String> langIds = new HashMap<>();

    private StsServiceDefinitionBuilder(String extensions) {
        this.extensions = extensions;
    }

    public static StsServiceDefinitionBuilder forExtensions(String extensions) {
        return new StsServiceDefinitionBuilder(extensions);
    }

    public StsServiceDefinitionBuilder withLanguageMapping(String extension, String languageId) {
        langIds.put(extension, languageId);
        return this;
    }

    public StsServiceDefinitionBuilder withServerListener() {
        this.serverListenerEnabled = true;
        return this;
    }

    public RawCommandServerDefinition build() {
        final String javaExecutable = isWindows() ? "java.exe" : "java";
        final String javaHome = System.getProperty("java.home");

        try {
            final StringBuilder classPathBuilder = new StringBuilder();
            final File root = new File(getClass().getResource("/").toURI().getPath());
            final Path javaHomePath = Paths.get(javaHome);

            classPathBuilder
                .append(new File(root.getParent(), "/server/language-server.jar").getPath());
            if (Prerequisities.isJava8()) {
                Path toolsJar = javaHomePath.resolve(Paths.get("lib", "tools.jar"));
                if (Files.exists(toolsJar)) {
                    classPathBuilder.append(File.pathSeparator).append(toolsJar);
                } else {
                    toolsJar = javaHomePath.resolve(Paths.get("..", "lib", "tools.jar"));
                    classPathBuilder.append(File.pathSeparator).append(toolsJar);
                }
            }
            //classPathBuilder.append("\'");
            final String javaExePath = javaHomePath.resolve(Paths.get("bin", javaExecutable))
                .toString();

            if (serverListenerEnabled) {
                return new StsListenableServerDefinition(extensions,
                        langIds,
                    new String[]{javaExePath, "-classpath", classPathBuilder.toString(), LAUNCHER});
            } else {
                return new StsServerDefinition(extensions,
                        langIds,
                    new String[]{javaExePath, "-classpath", classPathBuilder.toString(), LAUNCHER});
            }

        } catch (URISyntaxException e) {
            LOGGER.error(e);
            return null;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }
}
