package org.spring.tools.boot.java.ls.java;

import com.intellij.openapi.vfs.VirtualFile;
import org.springframework.ide.vscode.commons.protocol.java.Classpath.CPE;

import java.util.function.Function;

public class CommonMappings {

    public static CPE toBinaryCPE(VirtualFile file) {
        return CPE.binary(file.getPath().replace("!/", ""));
    }

    public static String firstOrNull(VirtualFile[] files, Function<VirtualFile, String> transformer) {
        if (files.length > 0) {
            return transformer.apply(files[0]);
        }
        return null;
    }

}
