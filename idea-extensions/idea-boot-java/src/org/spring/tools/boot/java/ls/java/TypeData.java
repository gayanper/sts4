package org.spring.tools.boot.java.ls.java;

import java.util.List;

public class TypeData {
    private String name;
    private String handleIdentifier;
    private String label;
    private String declaringType;
    private int flags;
    private String fqName;
    private boolean clazz;
    private boolean annotation;
    private boolean interfaze;
    private boolean enam;
    private String superClassName;
    private List<String> superInterfaceNames;
    private String bindingKey;
    private List<FieldData> fields;
    private List<MethodData> methods;
    private List<AnnotationData> annotations;
    private ClasspathEntryData classpathEntry:

    public List<FieldData> getFields() {
        return fields;
    }

    public void setFields(List<FieldData> fields) {
        this.fields = fields;
    }

    public List<MethodData> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodData> methods) {
        this.methods = methods;
    }

    public List<AnnotationData> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<AnnotationData> annotations) {
        this.annotations = annotations;
    }

    public ClasspathEntryData getClasspathEntry() {
        return classpathEntry;
    }

    public void setClasspathEntry(ClasspathEntryData classpathEntry) {
        this.classpathEntry = classpathEntry;
    }

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

    public String getFqName() {
        return fqName;
    }

    public void setFqName(String fqName) {
        this.fqName = fqName;
    }

    public boolean isClazz() {
        return clazz;
    }

    public void setClazz(boolean clazz) {
        this.clazz = clazz;
    }

    public boolean isAnnotation() {
        return annotation;
    }

    public void setAnnotation(boolean annotation) {
        this.annotation = annotation;
    }

    public boolean isInterfaze() {
        return interfaze;
    }

    public void setInterfaze(boolean interfaze) {
        this.interfaze = interfaze;
    }

    public boolean isEnam() {
        return enam;
    }

    public void setEnam(boolean enam) {
        this.enam = enam;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    public List<String> getSuperInterfaceNames() {
        return superInterfaceNames;
    }

    public void setSuperInterfaceNames(List<String> superInterfaceNames) {
        this.superInterfaceNames = superInterfaceNames;
    }

    public String getBindingKey() {
        return bindingKey;
    }

    public void setBindingKey(String bindingKey) {
        this.bindingKey = bindingKey;
    }
}
