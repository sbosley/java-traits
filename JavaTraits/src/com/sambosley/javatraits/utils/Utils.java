/**
 * Copyright 2014 Sam Bosley
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.sambosley.javatraits.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import com.sambosley.javatraits.processor.visitors.ImportGatheringTypeVisitor;

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
    
    
    public static <T extends TypeParameterElement> List<TypeName> mapTypeParameterElementsToTypeName(List<T> params, final String genericQualifier) {
        return map(params, new MapFunction<T, TypeName>() {
            @Override
            public TypeName map(TypeParameterElement arg) {
                return getTypeNameFromTypeMirror(arg.asType(), genericQualifier);
            }
        });
    }
    
    public static TypeName getTypeNameFromTypeMirror(TypeMirror mirror, String genericQualifier) {
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
            toReturn = new ClassName(mirrorString);
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
            String genericQualifier, boolean isAbstract, Modifier... extraModifiers) throws IOException {
        String name = nameOverride != null ? nameOverride : exec.getSimpleName().toString();
        List<TypeName> methodGenerics = Utils.mapTypeParameterElementsToTypeName(exec.getTypeParameters(), null);
        TypeName returnType = Utils.getTypeNameFromTypeMirror(exec.getReturnType(), null);
        if (!methodGenerics.contains(returnType) && returnType instanceof GenericName)
            ((GenericName) returnType).addQualifier(genericQualifier);
        List<Modifier> modifiers = new ArrayList<Modifier>();
        modifiers.add(Modifier.PUBLIC);
        if (extraModifiers != null)
            modifiers.addAll(Arrays.asList(extraModifiers));
        writer.beginMethodDeclaration(name, returnType, Arrays.asList(Modifier.PUBLIC), methodGenerics);
        List<String> argNames = emitMethodArguments(writer, exec, genericQualifier, methodGenerics);
        List<TypeName> thrownTypes = getThrownTypes(exec, genericQualifier, methodGenerics);
        writer.finishMethodDeclarationAndBeginMethodDefinition(thrownTypes, isAbstract);
        return argNames;
    }
    
    private static List<String> emitMethodArguments(JavaFileWriter writer, ExecutableElement exec, final String genericQualifier, final List<TypeName> methodGenerics) throws IOException {
        List<? extends VariableElement> arguments = exec.getParameters();
        List<TypeName> typeNames = Utils.map(arguments, new Utils.MapFunction<VariableElement, TypeName>() {
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
        List<String> argNames = Utils.map(arguments, new Utils.MapFunction<VariableElement, String>() {
            @Override
            public String map(VariableElement arg) {
                return arg.toString();
            }
        });
        if (exec.isVarArgs())
            typeNames.get(typeNames.size() - 1).setIsVarArgs(true);
        writer.addArgumentList(typeNames, null, argNames);
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
    
    @Deprecated
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
