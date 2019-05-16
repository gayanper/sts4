package org.spring.tools.boot.java.ls.java;

public class CPE {
    private static final String KIND_SOURCE = "source";
    private static final String KIND_BINARY = "binary";

    private String kind;
    private String path;
    private String outputFolder;
    private String sourceContainerUrl;
    private String javadocContainerUrl;
    private boolean isSystem;
    private boolean isOwn;

    private CPE() {
    }

    public static CPE source() {
        return newInstance(KIND_SOURCE);
    }

    public static CPE binary() {
        return newInstance(KIND_BINARY);
    }

    private static CPE newInstance(String kind) {
        CPE cpe = new CPE();
        cpe.setKind(kind);
        return cpe;
    }

    public String getKind() {
        return kind;
    }

    private void setKind(String kind) {
        this.kind = kind;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(String outputFolder) {
        this.outputFolder = outputFolder;
    }

    public String getSourceContainerUrl() {
        return sourceContainerUrl;
    }

    public void setSourceContainerUrl(String sourceContainerUrl) {
        this.sourceContainerUrl = sourceContainerUrl;
    }

    public String getJavadocContainerUrl() {
        return javadocContainerUrl;
    }

    public void setJavadocContainerUrl(String javadocContainerUrl) {
        this.javadocContainerUrl = javadocContainerUrl;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public void setSystem(boolean system) {
        isSystem = system;
    }

    public boolean isOwn() {
        return isOwn;
    }

    public void setOwn(boolean own) {
        isOwn = own;
    }
}
