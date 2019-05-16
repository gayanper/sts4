package org.spring.tools.boot.java.ls.java;

import java.util.Map;

public class AnnotationData {
    private String name;
    private String handleIdentifier;
    private String label;

    private String fqName;
    private Map<String, Object> valuePairs;

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

    public String getFqName() {
        return fqName;
    }

    public void setFqName(String fqName) {
        this.fqName = fqName;
    }

    public Map<String, Object> getValuePairs() {
        return valuePairs;
    }

    public void setValuePairs(Map<String, Object> valuePairs) {
        this.valuePairs = valuePairs;
    }
}
