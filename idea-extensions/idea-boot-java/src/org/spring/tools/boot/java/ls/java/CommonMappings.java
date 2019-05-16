package org.spring.tools.boot.java.ls.java;

import com.intellij.openapi.vfs.VirtualFile;

import java.util.function.Function;

public class CommonMappings {

    public static CPE toBinaryCPE(VirtualFile file) {
        CPE cpe = CPE.binary();
        cpe.setPath(file.getPath());
        return cpe;
    }

    public static String firstOrEmpty(VirtualFile[] files, Function<VirtualFile, String> transformer) {
        if (files.length > 0) {
            return transformer.apply(files[0]);
        }
        return "";
    }

}
