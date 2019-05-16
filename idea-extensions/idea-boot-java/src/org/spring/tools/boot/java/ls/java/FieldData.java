package org.spring.tools.boot.java.ls.java;

import java.util.List;

public class FieldData {
    private String name;
    private String handleIdentifier;
    private String label;

    private String declaringType;
    private int flags;

    private String bindingKey;
    private JavaTypeData type;
    private boolean enumConstant;
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

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public String getBindingKey() {
        return bindingKey;
    }

    public void setBindingKey(String bindingKey) {
        this.bindingKey = bindingKey;
    }

    public JavaTypeData getType() {
        return type;
    }

    public void setType(JavaTypeData type) {
        this.type = type;
    }

    public boolean isEnumConstant() {
        return enumConstant;
    }

    public void setEnumConstant(boolean enumConstant) {
        this.enumConstant = enumConstant;
    }

    public List<AnnotationData> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<AnnotationData> annotations) {
        this.annotations = annotations;
    }
}
