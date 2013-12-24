package com.sambosley.javatraits.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
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

    public static List<FullyQualifiedName> getClassFromAnnotation(Class<?> annotationClass, Element elem, String propertyName, Messager messager) {
        List<FullyQualifiedName> result = new ArrayList<FullyQualifiedName>();
        List<? extends AnnotationMirror> annotationMirrors = elem.getAnnotationMirrors();
        String annotationClassName = annotationClass.getName();
        for (AnnotationMirror mirror : annotationMirrors) {
            if (annotationClassName.equals(mirror.getAnnotationType().toString())) {
                for(Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                    if (propertyName.equals(entry.getKey().getSimpleName().toString())) {
                        Object value = entry.getValue().getValue();
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
                        break;
                    }
                }
            }
        }
        return result;
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
        }
    }

    public static List<String> emitMethodSignature(StringBuilder builder, ExecutableElement exec, String qualifyGenerics, boolean isAbstract) {
        List<String> argNames = new ArrayList<String>();
        builder.append("\tpublic ");
        if (isAbstract)
            builder.append("abstract ");
        builder.append(getSimpleTypeName(exec.getReturnType(), qualifyGenerics, false))
        .append(" ").append(exec.getSimpleName().toString())
        .append("(");
        List<? extends VariableElement> parameters = exec.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            VariableElement var = parameters.get(i);
            TypeMirror argType = var.asType();
            String typeString = getSimpleTypeName(argType, qualifyGenerics, false);
            if (argType.getKind() == TypeKind.ARRAY && exec.isVarArgs())
                typeString = typeString.replace("[]", "...");
            String argName = var.toString();
            argNames.add(argName);
            builder.append(typeString).append(" ").append(argName);
            if (i < parameters.size() - 1)
                builder.append(", ");
        }
        builder.append(")");
        return argNames;
    }
    
    public static String getSimpleTypeName(TypeMirror mirror, String qualifyByIfGeneric, boolean appendBounds) {
        String simpleName = getSimpleNameFromFullyQualifiedName(mirror.toString());
        String qualifiedName = qualifyByIfGeneric + "$" + simpleName;
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
