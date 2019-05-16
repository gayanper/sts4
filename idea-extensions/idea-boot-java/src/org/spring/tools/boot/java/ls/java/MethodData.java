package org.spring.tools.boot.java.ls.java;

import java.util.List;

public class MethodData {
    private String name;
    private String handleIdentifier;
    private String label;

    private String declaringType;
    private String flags;

    private String bindingKey;
    private boolean constructor;
    private JavaTypeData returnType;
    private List<JavaTypeData> parameters;
    private List<AnnotationData> annotations;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHandleIdentifier() {
        return handleIdentifier;
    }

    public void setHandleIdentifier(String handleIdentifier) {
        this.handleIdentifier = handleIdentifier;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDeclaringType() {
        return declaringType;
    }

    public void setDeclaringType(String declaringType) {
        this.declaringType = declaringType;
    }

    public String getFlags() {
        return flags;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

    public String getBindingKey() {
        return bindingKey;
    }

    public void setBindingKey(String bindingKey) {
        this.bindingKey = bindingKey;
    }

    public boolean isConstructor() {
        return constructor;
    }

    public void setConstructor(boolean constructor) {
        this.constructor = constructor;
    }

    public JavaTypeData getReturnType() {
        return returnType;
    }

    public void setReturnType(JavaTypeData returnType) {
        this.returnType = returnType;
    }

    public List<JavaTypeData> getParameters() {
        return parameters;
    }

    public void setParameters(List<JavaTypeData> parameters) {
        this.parameters = parameters;
    }

    public List<AnnotationData> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<AnnotationData> annotations) {
        this.annotations = annotations;
    }
}
