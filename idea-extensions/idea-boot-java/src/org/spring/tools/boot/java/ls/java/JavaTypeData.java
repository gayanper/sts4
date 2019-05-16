package org.spring.tools.boot.java.ls.java;

import java.util.Map;

public class JavaTypeData {
    private JavaTypeKind kind;
    private String name;
    private Map<Object, Object> extras;

    public JavaTypeKind getKind() {
        return kind;
    }

    public void setKind(JavaTypeKind kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Object, Object> getExtras() {
        return extras;
    }

    public void setExtras(Map<Object, Object> extras) {
        this.extras = extras;
    }
}
