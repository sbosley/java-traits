/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.annotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

public class Utils {

    public static final String OBJECT_CLASS_NAME = "java.lang.Object";

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

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

    public static <A, B> List<B> map(List<? extends A> list, MapFunction<A, B> mapFunction) {
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

    public static List<ClassName> getClassValuesFromAnnotationValue(AnnotationValue annotationValue) {
        List<ClassName> result = new ArrayList<ClassName>();
        Object value = annotationValue.getValue();
        if (value instanceof TypeMirror) {
            result.add(new ClassName(value.toString()));
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<? extends AnnotationValue> annotationValues = (List<? extends AnnotationValue>) value;
            for (AnnotationValue av : annotationValues) {
                Object itemValue = av.getValue();
                if (itemValue instanceof TypeMirror)
                    result.add(new ClassName(itemValue.toString()));
            }
        }
        return result;
    }

    public static List<ClassName> getClassValuesFromAnnotation(Class<?> annotationClass, Element elem, String propertyName, Messager messager) {
        AnnotationMirror mirror = findAnnotationMirror(elem, annotationClass);
        if (mirror != null) {
            AnnotationValue annotationValue = findAnnotationValue(mirror, propertyName);
            if (annotationValue != null)
                return getClassValuesFromAnnotationValue(annotationValue);
        }
        return new ArrayList<ClassName>();
    }

    public static void accumulateImportsFromExecutableElements(Set<ClassName> accumulate, List<? extends ExecutableElement> elems, Messager messager) {
        for (ExecutableElement exec : elems) {
            ImportGatheringTypeVisitor visitor = new ImportGatheringTypeVisitor(exec, messager);
            exec.asType().accept(visitor, accumulate);
        }
    }

    public static MethodSignature getMethodSignature(ExecutableElement exec, String genericQualifier) {
        String name = exec.getSimpleName().toString();
        MethodSignature result = new MethodSignature(name);

        List<TypeName> methodGenerics = Utils.mapTypeParameterElementsToTypeName(exec.getTypeParameters(), null);
        TypeName returnType = Utils.getTypeNameFromTypeMirror(exec.getReturnType(), null);
        if (!methodGenerics.contains(returnType) && returnType instanceof GenericName)
            ((GenericName) returnType).addQualifier(genericQualifier);
        result.setReturnType(returnType);

        List<TypeName> argTypeNames = getArgumentTypeNames(exec, genericQualifier, methodGenerics);
        result.addArgType(argTypeNames.toArray(new TypeName[argTypeNames.size()]));
        return result;
    }

    public static <T extends TypeParameterElement> List<TypeName> mapTypeParameterElementsToTypeName(List<T> params, final String genericQualifier) {
        return map(params, new MapFunction<T, TypeName>() {
            @Override
            public TypeName map(TypeParameterElement arg) {
                return getTypeNameFromTypeMirror(arg.asType(), genericQualifier);
            }
        });
    }

    public static TypeName getTypeNameFromTypeMirror(TypeMirror mirror, final String genericQualifier) {
        TypeKind kind = mirror.getKind();

        int arrayDepth = 0;
        while (kind == TypeKind.ARRAY) {
            ArrayType type = (ArrayType) mirror;
            arrayDepth++;
            mirror = type.getComponentType();
            kind = mirror.getKind();
        }

        String mirrorString = mirror.toString();
        TypeName toReturn;
        if (kind == TypeKind.TYPEVAR) {
            TypeVariable typeVariable = (TypeVariable) mirror;
            String genericName = getSimpleNameFromFullyQualifiedName(mirrorString);
            if (genericQualifier != null)
                genericName = genericQualifier + "$" + genericName;
            TypeMirror upperBoundMirror = typeVariable.getUpperBound();
            toReturn = getGenericName(genericName, genericQualifier, upperBoundMirror);
        } else if (kind == TypeKind.WILDCARD) {
            WildcardType wildcardType = (WildcardType) mirror;
            TypeMirror upperBoundMirror = wildcardType.getExtendsBound();
            toReturn = getGenericName("?", genericQualifier, upperBoundMirror);
        } else {
            List<TypeName> typeArgs = Collections.emptyList();
            if (mirror instanceof DeclaredType) {
                DeclaredType declaredMirror = (DeclaredType) mirror;
                List<? extends TypeMirror> declaredTypeArgs = declaredMirror.getTypeArguments();
                if (declaredTypeArgs.size() > 0) {
                    mirrorString = mirrorString.replaceAll("<.*>", "");
                    typeArgs = map(declaredTypeArgs, new MapFunction<TypeMirror, TypeName>() {
                        @Override
                        public TypeName map(TypeMirror arg) {
                            return getTypeNameFromTypeMirror(arg, genericQualifier);
                        }
                    });
                }
            }
            toReturn = new ClassName(mirrorString);
            ((ClassName) toReturn).setTypeArgs(typeArgs);
        }
        toReturn.setArrayDepth(arrayDepth);
        return toReturn;
    }

    private static GenericName getGenericName(String genericName, String genericQualifier, TypeMirror upperBoundMirror) {
        TypeName upperBound = null;
        if (upperBoundMirror != null && !OBJECT_CLASS_NAME.equals(upperBoundMirror.toString()))
            upperBound = getTypeNameFromTypeMirror(upperBoundMirror, genericQualifier);
        return new GenericName(genericName, upperBound);
    }

    public static List<String> beginMethodDeclarationForExecutableElement(JavaFileWriter writer, ExecutableElement exec, String nameOverride,
            String genericQualifier, boolean isAbstract, Modifier... modifiers) throws IOException {
        String name = nameOverride != null ? nameOverride : exec.getSimpleName().toString();
        List<TypeName> methodGenerics = Utils.mapTypeParameterElementsToTypeName(exec.getTypeParameters(), null);
        TypeName returnType = Utils.getTypeNameFromTypeMirror(exec.getReturnType(), null);
        qualifyReturnTypeGenerics(methodGenerics, returnType, genericQualifier);

        writer.beginMethodDeclaration(name, returnType, Arrays.asList(modifiers), methodGenerics);
        List<String> argNames = writeMethodArguments(writer, exec, genericQualifier, methodGenerics);
        List<TypeName> thrownTypes = getThrownTypes(exec, genericQualifier, methodGenerics);
        writer.finishMethodDeclarationAndBeginMethodDefinition(thrownTypes, isAbstract);
        return argNames;
    }

    private static void qualifyReturnTypeGenerics(List<TypeName> methodGenerics, TypeName returnType, String genericQualifier) {
        if (!methodGenerics.contains(returnType) && returnType instanceof GenericName)
            ((GenericName) returnType).addQualifier(genericQualifier);
        if (returnType instanceof ClassName) {
            ClassName returnClass = (ClassName) returnType;
            for (TypeName nestedType : returnClass.getTypeArgs())
                qualifyReturnTypeGenerics(methodGenerics, nestedType, genericQualifier);
        }
    }

    private static List<TypeName> getArgumentTypeNames(ExecutableElement exec, final String genericQualifier, final List<TypeName> methodGenerics) {
        List<TypeName> typeNames = Utils.map(exec.getParameters(), new Utils.MapFunction<VariableElement, TypeName>() {
            @Override
            public TypeName map(VariableElement arg) {
                return Utils.getTypeNameFromTypeMirror(arg.asType(), null);
            }
        });
        Utils.map(typeNames, new Utils.MapFunction<TypeName, Void>() {
            @Override
            public Void map(TypeName arg) {
                if (!methodGenerics.contains(arg) && arg instanceof GenericName)
                    ((GenericName) arg).addQualifier(genericQualifier);
                return null;
            }
        });
        return typeNames;
    }

    private static List<String> writeMethodArguments(JavaFileWriter writer, ExecutableElement exec, final String genericQualifier, final List<TypeName> methodGenerics) throws IOException {
        List<? extends VariableElement> arguments = exec.getParameters();
        List<TypeName> typeNames = getArgumentTypeNames(exec, genericQualifier, methodGenerics);
        List<String> argNames = Utils.map(arguments, new Utils.MapFunction<VariableElement, String>() {
            @Override
            public String map(VariableElement arg) {
                return arg.toString();
            }
        });
        if (exec.isVarArgs())
            typeNames.get(typeNames.size() - 1).setIsVarArgs(true);
        writer.addArgumentList(typeNames, argNames);
        return argNames;
    }

    private static List<TypeName> getThrownTypes(ExecutableElement exec, final String genericQualifier, final List<TypeName> methodGenerics) {
        List<? extends TypeMirror> thrownTypeMirrors = exec.getThrownTypes();
        List<TypeName> thrownTypes = Utils.map(thrownTypeMirrors, new Utils.MapFunction<TypeMirror, TypeName>() {
            @Override
            public TypeName map(TypeMirror arg) {
                return Utils.getTypeNameFromTypeMirror(arg, null);
            }
        });
        Utils.map(thrownTypes, new Utils.MapFunction<TypeName, Void>() {
            @Override
            public Void map(TypeName arg) {
                if (!methodGenerics.contains(arg) && arg instanceof GenericName)
                    ((GenericName) arg).addQualifier(genericQualifier);
                return null;
            }
        });
        return thrownTypes;
    }

}
