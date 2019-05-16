package org.spring.tools.boot.java.ls.java;

import com.intellij.lang.jvm.annotation.JvmAnnotationArrayValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationClassValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue;
import com.intellij.lang.jvm.annotation.JvmAnnotationEnumFieldValue;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TypeProvider {
    private static Map<Class, Function<JvmAnnotationAttributeValue, Object>> attributeValueMappings = new HashMap<>();

    static {
        attributeValueMappings.put(JvmAnnotationConstantValue.class,
                v -> ((JvmAnnotationConstantValue) v).getConstantValue());
        attributeValueMappings.put(JvmAnnotationEnumFieldValue.class,
                v -> ((JvmAnnotationEnumFieldValue) v).getFieldName());
        attributeValueMappings.put(JvmAnnotationClassValue.class,
                v -> ((JvmAnnotationClassValue) v).getQualifiedName());
        attributeValueMappings.put(JvmAnnotationArrayValue.class,
                v -> ((JvmAnnotationArrayValue) v).getValues().stream()
                        .map(v1 -> attributeValueMappings.get(v1.getClass()).apply(v1)).toArray());
    }

    private Project project;

    public TypeProvider(Project project) {
        this.project = project;
    }

    public TypeData typeDataFor(String typeBinding) {
        PsiClass psiClass = ClassUtil.findPsiClassByJVMName(PsiManager.getInstance(project), typeBinding);
        TypeData data = new TypeData();
        data.setName(psiClass.getName());
        data.setLabel(data.getName());

        if (PsiUtil.isInnerClass(psiClass)) {
            //todo data.setDeclaringType(psiClass.getContainingClass().getQualifiedName());
        }
        //todo data.setFlags(PsiUtil.getAccessLevel(psiClass.getModifierList()));

        data.setFqName(psiClass.getQualifiedName());
        data.setClazz(PsiUtil.isLocalClass(psiClass));
        data.setAnnotation(psiClass.isAnnotationType());
        data.setInterfaze(psiClass.isInterface());
        data.setEnam(psiClass.isEnum());
        if (psiClass.getSuperClass() != null) {
            data.setSuperClassName(psiClass.getSuperClass().getName());
        }
        data.setSuperInterfaceNames(Arrays.stream(psiClass.getInterfaceTypes())
                .map(i -> i.getName()).collect(Collectors.toList()));

        data.setBindingKey(typeBinding);
        data.setFields(mapFields(psiClass.getFields()));
        data.setMethods(mapMethods(psiClass.getMethods()));
        data.setAnnotations(mapAnnotations(psiClass.getAnnotations()));
        data.setClasspathEntry(findCPE(psiClass));
        return data;
    }

    private ClasspathEntryData findCPE(PsiClass psiClass) {
        VirtualFile virtualFile = psiClass.getContainingFile().getVirtualFile();
        Library library = LibraryUtil.findLibraryByClass(psiClass.getQualifiedName(), psiClass.getProject());
        Optional<CPE> first = Arrays.stream(library.getFiles(OrderRootType.CLASSES))
                .map(CommonMappings::toBinaryCPE).findFirst();
        if (first.isPresent()) {
            ClasspathEntryData data = new ClasspathEntryData();
            data.setModule(library.getName());
            data.setEntry(first.get());
            return data;
        }
        return null;
    }

    private List<AnnotationData> mapAnnotations(PsiAnnotation[] annotations) {
        return Arrays.stream(annotations).map(a -> {
            AnnotationData data = new AnnotationData();
            data.setFqName(a.getQualifiedName());
            data.setName(ClassUtil.extractClassName(a.getQualifiedName()));
            data.setLabel(data.getName());

            Map<String, Object> values = new HashMap<>();
            for (JvmAnnotationAttribute attribute : a.getAttributes()) {
                Function<JvmAnnotationAttributeValue, Object> mapper =
                        attributeValueMappings.get(attribute.getAttributeValue().getClass());
                if (mapper != null) {
                    values.put(attribute.getAttributeName(), mapper.apply(attribute.getAttributeValue()));
                }
            }
            data.setValuePairs(values);
            return data;
        }).collect(Collectors.toList());
    }

    private List<FieldData> mapFields(PsiField[] fields) {
        return Arrays.stream(fields).map(f -> {
            FieldData data = new FieldData();
            data.setName(f.getName());
            data.setLabel(data.getName());

            //todo data.setDeclaringType
            //todo data.setFlags(PsiUtil.getAccessLevel(f.getModifierList()));

            //todo  data.setBindingKey();
            //todo data.setType();
            data.setEnumConstant(f instanceof PsiEnumConstant);
            data.setAnnotations(mapAnnotations(f.getAnnotations()));
            return data;
        }).collect(Collectors.toList());
    }

    private List<MethodData> mapMethods(PsiMethod[] methods) {
        return Arrays.stream(methods).map(m -> {
            MethodData data = new MethodData();
            data.setName(m.getName());
            data.setLabel(data.getName());

            //todo data.setDeclaringType
            //todo data.setFlags(PsiUtil.getAccessLevel(m.getModifierList()));

            //todo  data.setBindingKey();
            data.setConstructor(m.isConstructor());
            //todo data.setReturnType();
            //todo data.setParameters();
            data.setAnnotations(mapAnnotations(m.getAnnotations()));
            return data;
        }).collect(Collectors.toList());
    }
}
