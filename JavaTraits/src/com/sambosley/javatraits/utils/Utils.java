/**
 * Copyright 2014 Sam Bosley
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.sambosley.javatraits.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import com.sambosley.javatraits.processor.visitors.ImportGatheringTypeVisitor;

public class Utils {

    public static final String OBJECT_CLASS_NAME = "java.lang.Object";
    
    public static String getPackageFromFullyQualifiedName(String name) {
        int split = getFQNSplitIndex(name);
        if (split < 0)
            return "";
        return name.substring(0, split);
    }

    public static String getSimpleNameFromFullyQualifiedName(String name) {
        int split = getFQNSplitIndex(name);
        if (split < 0)
            return name;
        return name.substring(split + 1);
    }

    private static int getFQNSplitIndex(String name) {
        return name.lastIndexOf('.');
    }

    public static interface MapFunction<A, B> {
        public B map(A arg);
    }

    public static <A, B> List<B> map(List<A> list, MapFunction<A, B> mapFunction) {
        List<B> result = new ArrayList<B>();
        for (A elem : list) {
            result.add(mapFunction.map(elem));
        }
        return result;
    }
    
    public static AnnotationMirror findAnnotationMirror(Element elem, Class<?> annotationClass) {
        List<? extends AnnotationMirror> annotationMirrors = elem.getAnnotationMirrors();
        String annotationClassName = annotationClass.getName();
        for (AnnotationMirror mirror : annotationMirrors)
            if (annotationClassName.equals(mirror.getAnnotationType().toString()))
                return mirror;
        return null;
    }
    
    public static AnnotationValue findAnnotationValue(AnnotationMirror mirror, String propertyName) {
        for(Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet())
            if (propertyName.equals(entry.getKey().getSimpleName().toString()))
                return entry.getValue();
        return null;
    }
    
    public static List<FullyQualifiedName> getClassValuesFromAnnotationValue(AnnotationValue annotationValue) {
        List<FullyQualifiedName> result = new ArrayList<FullyQualifiedName>();
        Object value = annotationValue.getValue();
        if (value instanceof TypeMirror) {
            result.add(new FullyQualifiedName(value.toString()));
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<? extends AnnotationValue> annotationValues = (List<? extends AnnotationValue>) value;
            for (AnnotationValue av : annotationValues) {
                Object itemValue = av.getValue();
                if (itemValue instanceof TypeMirror)
                    result.add(new FullyQualifiedName(itemValue.toString()));
            }
        }
        return result;
    }

    public static List<FullyQualifiedName> getClassValuesFromAnnotation(Class<?> annotationClass, Element elem, String propertyName, Messager messager) {
        AnnotationMirror mirror = findAnnotationMirror(elem, annotationClass);
        if (mirror != null) {
            AnnotationValue annotationValue = findAnnotationValue(mirror, propertyName);
            if (annotationValue != null)
                return getClassValuesFromAnnotationValue(annotationValue);
        }
        return new ArrayList<FullyQualifiedName>();
    }

    public static void accumulateImportsFromExecutableElements(Set<String> accumulate, List<? extends ExecutableElement> elems, Messager messager) {
        for (ExecutableElement exec : elems) {
            ImportGatheringTypeVisitor visitor = new ImportGatheringTypeVisitor(exec, messager);
            TypeMirror returnType = exec.getReturnType();
            returnType.accept(visitor, accumulate);
            List<? extends VariableElement> parameters = exec.getParameters();
            for (VariableElement var : parameters) {
                var.asType().accept(visitor, accumulate);
            }
            List<? extends TypeMirror> thrownTypes = exec.getThrownTypes();
            for (TypeMirror thrown : thrownTypes) {
                thrown.accept(visitor, accumulate);
            }
        }
    }
    
    public static String getMethodNameFromSignature(String methodSignature) {
        String toReturn = methodSignature;
        int indexOfParen = methodSignature.indexOf('(');
        if (indexOfParen >= 0)
            toReturn = toReturn.substring(0, indexOfParen).trim();
        
        int lastSpace = toReturn.lastIndexOf(' ');
        if (lastSpace >= 0)
            toReturn = toReturn.substring(lastSpace + 1); 
        return toReturn;
    }
    
    public static String getMethodSignature(ExecutableElement exec) {
        String toReturn = exec.toString();
        String simpleName = exec.getSimpleName().toString();
        int simpleStart = toReturn.indexOf(simpleName);
        if (simpleStart >= 0) {
            toReturn = toReturn.substring(simpleStart).trim();
            String returnType = Utils.getSimpleNameFromFullyQualifiedName(exec.getReturnType().toString()); 
            toReturn = returnType + " " + toReturn;
        }
        return toReturn;
    }

    public static List<String> emitMethodSignature(StringBuilder builder, ExecutableElement exec, String methodNamePrefix, String qualifyGenerics, boolean isAbstract, boolean isFinal) {
        List<String> argNames = new ArrayList<String>();
        builder.append("\tpublic ");
        if (isAbstract)
            builder.append("abstract ");
        if (isFinal)
            builder.append("final ");
        Set<String> methodTypeParams = new HashSet<String>();
        List<? extends TypeParameterElement> typeParameters = exec.getTypeParameters();
        if (typeParameters.size() > 0) {
            builder.append("<");
            for (int i = 0; i < typeParameters.size(); i++) {
                TypeMirror type = typeParameters.get(i).asType();
                String typeNameWithBounds = getSimpleTypeName(type, null, true); 
                String typeNameWithoutBounds = getSimpleTypeName(type, null, false); 
                builder.append(typeNameWithBounds);
                if (i < typeParameters.size() - 1)
                    builder.append(", ");
                methodTypeParams.add(typeNameWithoutBounds);
            }
            builder.append("> ");
        }
        
        String simpleReturnTypeName = getSimpleNameFromFullyQualifiedName(exec.getReturnType().toString());
        String qualifyReturnType = methodTypeParams.contains(simpleReturnTypeName) ? null : qualifyGenerics;
        builder.append(getSimpleTypeName(exec.getReturnType(), qualifyReturnType, false))
        .append(" ");
        if (methodNamePrefix != null)
            builder.append(methodNamePrefix);
        builder.append(exec.getSimpleName().toString())
        .append("(");
        List<? extends VariableElement> parameters = exec.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement var = parameters.get(i);
            TypeMirror argType = var.asType();
            String simpleTypeName = getSimpleNameFromFullyQualifiedName(argType.toString());
            String qualifyArgType = methodTypeParams.contains(simpleTypeName) ? null : qualifyGenerics;
            String typeString = getSimpleTypeName(argType, qualifyArgType, false);
            if (argType.getKind() == TypeKind.ARRAY && exec.isVarArgs())
                typeString = typeString.replace("[]", "...");
            String argName = var.toString();
            argNames.add(argName);
            builder.append(typeString).append(" ").append(argName);
            if (i < parameters.size() - 1)
                builder.append(", ");
        }
        builder.append(")");
        List<? extends TypeMirror> thrownTypes = exec.getThrownTypes();
        if (!thrownTypes.isEmpty()) {
            builder.append(" throws ");
            for (int i = 0; i < thrownTypes.size(); i++) {
                TypeMirror type = thrownTypes.get(i);
                String simpleTypeName = getSimpleNameFromFullyQualifiedName(type.toString());
                String qualifyArgType = methodTypeParams.contains(simpleTypeName) ? null : qualifyGenerics;
                String typeString = getSimpleTypeName(type, qualifyArgType, false);
                builder.append(typeString);
                if (i < thrownTypes.size() - 1)
                    builder.append(", ");
            }
        }
        return argNames;
    }
    
    public static String getSimpleTypeName(TypeMirror mirror, String qualifyByIfGeneric, boolean appendBounds) {
        String simpleName = getSimpleNameFromFullyQualifiedName(mirror.toString());
        String qualifiedName = qualifyByIfGeneric == null ? simpleName : qualifyByIfGeneric + "$" + simpleName;
        TypeVariable typeVariableIfGeneric = null;
        
        if (mirror instanceof TypeVariable) {
            typeVariableIfGeneric = (TypeVariable) mirror;
        } else if (mirror instanceof ArrayType) {
            ArrayType arrType = (ArrayType) mirror;
            if (arrType.getComponentType() instanceof TypeVariable) {
                typeVariableIfGeneric = (TypeVariable) arrType.getComponentType();
            }
        }
        if (typeVariableIfGeneric != null && appendBounds) {
            TypeMirror upperBound = typeVariableIfGeneric.getUpperBound();
            if (!Utils.OBJECT_CLASS_NAME.equals(upperBound.toString())) {
                String qualifiedUpperBound = Utils.getSimpleNameFromFullyQualifiedName(upperBound.toString());
                if (upperBound instanceof TypeVariable)
                    qualifiedUpperBound = qualifyByIfGeneric + "$" + qualifiedUpperBound;
                qualifiedName += " extends " + qualifiedUpperBound;
            }
        }
        
        return typeVariableIfGeneric != null ? qualifiedName : simpleName;
    }

}
