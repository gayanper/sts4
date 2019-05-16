package org.spring.tools.boot.java.ls.java;

public class JavaDataParams {
    private String projectUri;
    private String bindingKey;
    private boolean lookInOtherProjects;

    public String getProjectUri() {
        return projectUri;
    }

    public void setProjectUri(String projectUri) {
        this.projectUri = projectUri;
    }

    public String getBindingKey() {
        return bindingKey;
    }

    public void setBindingKey(String bindingKey) {
        this.bindingKey = bindingKey;
    }

    public boolean isLookInOtherProjects() {
        return lookInOtherProjects;
    }

    public void setLookInOtherProjects(boolean lookInOtherProjects) {
        this.lookInOtherProjects = lookInOtherProjects;
    }
}
