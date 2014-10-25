/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.annotations.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import javax.lang.model.util.Types;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.model.GenericName;
import com.yahoo.annotations.model.MethodSignature;
import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.visitors.ImportGatheringTypeVisitor;
import com.yahoo.annotations.writer.parameters.MethodDeclarationParameters;

public class Utils {

    public static final String OBJECT_CLASS_NAME = "java.lang.Object";

    private Messager messager;
    private Types types;

    public Utils(Messager messager, Types types) {
        this.messager = messager;
        this.types = types;
    }

    public Messager getMessager() {
        return messager;
    }

    public Types getTypes() {
        return types;
    }

    public void accumulateImportsFromExecutableElements(Set<DeclaredTypeName> accumulate, List<? extends ExecutableElement> elems) {
        for (ExecutableElement exec : elems) {
            ImportGatheringTypeVisitor visitor = new ImportGatheringTypeVisitor(exec, messager, this);
            exec.asType().accept(visitor, accumulate);
        }
    }

    public MethodSignature executableElementToMethodSignature(ExecutableElement exec) {
        return executableElementToMethodSignature(exec, null);
    }
    
    public MethodSignature executableElementToMethodSignature(ExecutableElement exec, String genericQualifier) {
        String name = exec.getSimpleName().toString();
        MethodSignature result = new MethodSignature(name);

        List<TypeName> methodGenerics = typeParameterElementsToTypeNames(exec.getTypeParameters(), null);
        TypeName returnType = getTypeNameFromTypeMirror(exec.getReturnType(), null);
        if (!methodGenerics.contains(returnType) && returnType instanceof GenericName && genericQualifier != null) {
            ((GenericName) returnType).addQualifier(genericQualifier);
        }
        result.setReturnType(returnType);

        List<TypeName> argTypeNames = getArgumentTypeNames(exec, genericQualifier, methodGenerics);
        result.addArgType(argTypeNames.toArray(new TypeName[argTypeNames.size()]));
        return result;
    }

    public TypeName typeParameterElementToTypeName(TypeParameterElement elem) {
        return typeParameterElementToTypeName(elem, null);
    }
    
    public TypeName typeParameterElementToTypeName(TypeParameterElement elem, String genericQualifier) {
        return getTypeNameFromTypeMirror(elem.asType(), genericQualifier);
    }
    
    public <T extends TypeParameterElement> List<TypeName> typeParameterElementsToTypeNames(List<T> params) {
        return typeParameterElementsToTypeNames(params, null);
    }
    
    public <T extends TypeParameterElement> List<TypeName> typeParameterElementsToTypeNames(List<T> params, final String genericQualifier) {
        return map(params, new Mapper<T, TypeName>() {
            @Override
            public TypeName map(TypeParameterElement arg) {
                return typeParameterElementToTypeName(arg, genericQualifier);
            }
        });
    }

    public TypeName getTypeNameFromTypeMirror(TypeMirror mirror) {
        return getTypeNameFromTypeMirror(mirror, null);
    }
    
    public TypeName getTypeNameFromTypeMirror(TypeMirror mirror, final String genericQualifier) {
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
            if (genericQualifier != null) {
                genericName = genericQualifier + GenericName.GENERIC_QUALIFIER_SEPARATOR + genericName;
            }
            toReturn = getGenericName(genericName, genericQualifier, typeVariable, typeVariable.getUpperBound(), null);
        } else if (kind == TypeKind.WILDCARD) {
            WildcardType wildcardType = (WildcardType) mirror;
            toReturn = getGenericName(GenericName.WILDCARD_CHAR, genericQualifier, wildcardType, wildcardType.getExtendsBound(), wildcardType.getSuperBound());
        } else {
            List<TypeName> typeArgs = Collections.emptyList();
            if (mirror instanceof DeclaredType) {
                DeclaredType declaredMirror = (DeclaredType) mirror;
                List<? extends TypeMirror> declaredTypeArgs = declaredMirror.getTypeArguments();
                if (declaredTypeArgs.size() > 0) {
                    mirrorString = mirrorString.replaceAll("<.*>", "");
                    typeArgs = map(declaredTypeArgs, new Mapper<TypeMirror, TypeName>() {
                        @Override
                        public TypeName map(TypeMirror arg) {
                            return getTypeNameFromTypeMirror(arg, genericQualifier);
                        }
                    });
                }
            }
            toReturn = new DeclaredTypeName(mirrorString);
            ((DeclaredTypeName) toReturn).setTypeArgs(typeArgs);
        }
        toReturn.setArrayDepth(arrayDepth);
        return toReturn;
    }

    private GenericName getGenericName(String genericName, String genericQualifier, TypeMirror fromMirror, TypeMirror extendsBoundMirror, TypeMirror superBoundMirror) {
        List<TypeName> extendsBound = null;
        if (extendsBoundMirror != null && !OBJECT_CLASS_NAME.equals(extendsBoundMirror.toString())) {
            extendsBound = getUpperBoundsFromTypeMirror(fromMirror, extendsBoundMirror, genericQualifier);
        }
        TypeName superBound = null;
        if (superBoundMirror != null && !OBJECT_CLASS_NAME.equals(superBoundMirror.toString())) {
            superBound = getTypeNameFromTypeMirror(superBoundMirror, genericQualifier);
        }
        return new GenericName(genericName, extendsBound, superBound);
    }

    private List<TypeName> getUpperBoundsFromTypeMirror(TypeMirror sourceMirror, TypeMirror extendsBoundMirror, final String genericQualifier) {
        List<? extends TypeMirror> upperBounds = getUpperBoundMirrors(sourceMirror, extendsBoundMirror);
        return map(upperBounds, new Mapper<TypeMirror, TypeName>() {
            @Override
            public TypeName map(TypeMirror arg) {
                return getTypeNameFromTypeMirror(arg, genericQualifier);
            }
        });
    }

    public List<? extends TypeMirror> getUpperBoundMirrors(TypeMirror sourceMirror, TypeMirror extendsBoundMirror) {
        List<TypeMirror> result = new ArrayList<TypeMirror>();
        if (extendsBoundMirror == null) {
            return result;
        }

        if (extendsBoundMirror instanceof DeclaredType) {
            if (extendsBoundMirror.toString().contains("&")) { // Is intersection type
                addSupertypesToUpperBoundList(result, extendsBoundMirror);
            } else {
                result.add(extendsBoundMirror);
            }
        } else if (types.isSameType(sourceMirror, extendsBoundMirror)) { // Workaround for an Eclipse bug that mishandles intersection types
            addSupertypesToUpperBoundList(result, extendsBoundMirror);
        } else {
            result.add(extendsBoundMirror);
        }
        return result;
    }

    private void addSupertypesToUpperBoundList(List<TypeMirror> list, TypeMirror upperBoundMirror) {
        List<? extends TypeMirror> supertypes = types.directSupertypes(upperBoundMirror);
        for (TypeMirror t : supertypes) {
            if (!OBJECT_CLASS_NAME.equals(t.toString())) {
                list.add(t);
            }
        }
    }
    
    public MethodDeclarationParameters methodDeclarationParamsFromExecutableElement(ExecutableElement exec, Modifier... modifiers) {
        return methodDeclarationParamsFromExecutableElement(exec, null, modifiers);
    }
    
    public MethodDeclarationParameters methodDeclarationParamsFromExecutableElement(ExecutableElement exec, String nameOverride, Modifier... modifiers) {
        return methodDeclarationParamsFromExecutableElement(exec, nameOverride, null, modifiers);
    }
    
    public MethodDeclarationParameters methodDeclarationParamsFromExecutableElement(ExecutableElement exec, String nameOverride,
            String genericQualifier, Modifier... modifiers) {
        String name = nameOverride != null ? nameOverride : exec.getSimpleName().toString();
        List<TypeName> methodGenerics = typeParameterElementsToTypeNames(exec.getTypeParameters(), null);
        TypeName returnType = getTypeNameFromTypeMirror(exec.getReturnType(), null);
        qualifyReturnTypeGenerics(methodGenerics, returnType, genericQualifier);

        Pair<List<TypeName>, List<String>> arguments = getMethodArgumentsFromExecutableElement(exec, genericQualifier, methodGenerics);

        MethodDeclarationParameters params = new MethodDeclarationParameters()
            .setMethodName(name)
            .setReturnType(returnType)
            .setModifiers(Arrays.asList(modifiers))
            .setMethodGenerics(methodGenerics)
            .setArgumentTypes(arguments.getLeft())
            .setArgumentNames(arguments.getRight())
            .setThrowsTypes(getThrownTypes(exec, genericQualifier, methodGenerics));
        
        return params;
    }

    private void qualifyReturnTypeGenerics(List<TypeName> methodGenerics, TypeName returnType, String genericQualifier) {
        if (!methodGenerics.contains(returnType) && returnType instanceof GenericName) {
            ((GenericName) returnType).addQualifier(genericQualifier);
        }
        if (returnType instanceof DeclaredTypeName) {
            DeclaredTypeName returnClass = (DeclaredTypeName) returnType;
            for (TypeName nestedType : returnClass.getTypeArgs()) {
                qualifyReturnTypeGenerics(methodGenerics, nestedType, genericQualifier);
            }
        }
    }

    private List<TypeName> getArgumentTypeNames(ExecutableElement exec, final String genericQualifier, final List<TypeName> methodGenerics) {
        List<TypeName> typeNames = map(exec.getParameters(), new Mapper<VariableElement, TypeName>() {
            @Override
            public TypeName map(VariableElement arg) {
                return getTypeNameFromTypeMirror(arg.asType(), null);
            }
        });
        map(typeNames, new Mapper<TypeName, Void>() {
            @Override
            public Void map(TypeName arg) {
                if (!methodGenerics.contains(arg) && arg instanceof GenericName) {
                    ((GenericName) arg).addQualifier(genericQualifier);
                }
                return null;
            }
        });
        return typeNames;
    }

    private Pair<List<TypeName>, List<String>> getMethodArgumentsFromExecutableElement(ExecutableElement exec, final String genericQualifier, final List<TypeName> methodGenerics) {
        List<? extends VariableElement> arguments = exec.getParameters();
        List<TypeName> typeNames = getArgumentTypeNames(exec, genericQualifier, methodGenerics);
        List<String> argNames = map(arguments, new Mapper<VariableElement, String>() {
            @Override
            public String map(VariableElement arg) {
                return arg.toString();
            }
        });
        if (exec.isVarArgs()) {
            typeNames.get(typeNames.size() - 1).setIsVarArgs(true);
        }
        return Pair.create(typeNames, argNames);
    }

    private List<TypeName> getThrownTypes(ExecutableElement exec, final String genericQualifier, final List<TypeName> methodGenerics) {
        List<? extends TypeMirror> thrownTypeMirrors = exec.getThrownTypes();
        List<TypeName> thrownTypes = Utils.map(thrownTypeMirrors, new Utils.Mapper<TypeMirror, TypeName>() {
            @Override
            public TypeName map(TypeMirror arg) {
                return getTypeNameFromTypeMirror(arg, null);
            }
        });
        Utils.map(thrownTypes, new Utils.Mapper<TypeName, Void>() {
            @Override
            public Void map(TypeName arg) {
                if (!methodGenerics.contains(arg) && arg instanceof GenericName) {
                    ((GenericName) arg).addQualifier(genericQualifier);
                }
                return null;
            }
        });
        return thrownTypes;
    }

    // --- static methods
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static String getPackageFromFullyQualifiedName(String name) {
        int split = getFQNSplitIndex(name);
        if (split < 0) {
            return "";
        }
        return name.substring(0, split);
    }

    public static String getSimpleNameFromFullyQualifiedName(String name) {
        int split = getFQNSplitIndex(name);
        if (split < 0) {
            return name;
        }
        return name.substring(split + 1);
    }

    private static int getFQNSplitIndex(String name) {
        return name.lastIndexOf('.');
    }

    public static interface Mapper<A, B> {
        public B map(A arg);
    }

    public static <A, B> List<B> map(List<? extends A> list, Mapper<A, B> mapFunction) {
        List<B> result = new ArrayList<B>();
        for (A elem : list) {
            result.add(mapFunction.map(elem));
        }
        return result;
    }

    public static boolean deepCompareTypeList(List<? extends TypeName> l1, List<? extends TypeName> l2) {
        if (l1.size() != l2.size()) {
            return false;
        }
        for (int i = 0; i < l1.size(); i++) {
            if (!deepCompareTypes(l1.get(i), l2.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean deepCompareTypes(TypeName t1, TypeName t2) {
        if (!t1.equals(t2)) {
            return false;
        }
        if (!(t1.getArrayDepth() == t2.getArrayDepth() && t1.isVarArgs() == t2.isVarArgs())) {
            return false;
        }
        if (t1 instanceof DeclaredTypeName && t2 instanceof DeclaredTypeName) {
            return deepCompareTypeList(((DeclaredTypeName) t1).getTypeArgs(), ((DeclaredTypeName) t2).getTypeArgs());
        }
        return true;
    }

    public AnnotationValue getAnnotationValue(Element elem, Class<?> annotationClass, String propertyName) {
        AnnotationMirror mirror = getAnnotationMirror(elem, annotationClass);
        return getAnnotationValueFromMirror(mirror, propertyName);
    }
    
    public AnnotationMirror getAnnotationMirror(Element elem, Class<?> annotationClass) {
        List<? extends AnnotationMirror> annotationMirrors = elem.getAnnotationMirrors();
        String annotationClassName = annotationClass.getName();
        for (AnnotationMirror mirror : annotationMirrors) {
            if (annotationClassName.equals(mirror.getAnnotationType().toString())) {
                return mirror;
            }
        }
        return null;
    }

    public AnnotationValue getAnnotationValueFromMirror(AnnotationMirror mirror, String propertyName) {
        if (mirror != null) {
            for(Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                if (propertyName.equals(entry.getKey().getSimpleName().toString())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }
    
    public List<DeclaredTypeName> getClassValuesFromAnnotation(Element elem, Class<?> annotationClass, String propertyName) {
        AnnotationValue annotationValue = getAnnotationValue(elem, annotationClass, propertyName);
        return getClassValuesFromAnnotationValue(annotationValue);
    }
    
    public List<TypeMirror> getClassMirrorsFromAnnotation(Element elem, Class<?> annotationClass, String propertyName) {
        AnnotationValue annotationValue = getAnnotationValue(elem, annotationClass, propertyName);
        return getClassMirrorsFromAnnotationValue(annotationValue);
    }
    
    public List<String> getStringValuesFromAnnotation(Element elem, Class<?> annotationClass, String propertyName) {
        AnnotationValue annotationValue = getAnnotationValue(elem, annotationClass, propertyName);
        return getStringValuesFromAnnotationValue(annotationValue);
    }

    public List<DeclaredTypeName> getClassValuesFromAnnotationValue(AnnotationValue annotationValue) {
        return mapValuesFromAnnotationValue(annotationValue, TypeMirror.class, new Mapper<TypeMirror, DeclaredTypeName>() {
            @Override
            public DeclaredTypeName map(TypeMirror arg) {
                return new DeclaredTypeName(arg.toString());
            }
        });
    }
    
    public List<TypeMirror> getClassMirrorsFromAnnotationValue(AnnotationValue annotationValue) {
        return getValuesFromAnnotationValue(annotationValue, TypeMirror.class);
    }

    public List<String> getStringValuesFromAnnotationValue(AnnotationValue annotationValue) {
        return getValuesFromAnnotationValue(annotationValue, String.class);
    }
    
    private <T> List<T> getValuesFromAnnotationValue(AnnotationValue annotationValue, Class<T> valueClass) {
        return mapValuesFromAnnotationValue(annotationValue, valueClass, new Mapper<T, T>() {
            @Override
            public T map(T arg) {
                return arg;
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    private <V, T> List<T> mapValuesFromAnnotationValue(AnnotationValue annotationValue, Class<V> valueClass, Mapper<V, T> mapResult) {
        List<T> result = new ArrayList<T>();
        if (annotationValue != null) {
            Object value = annotationValue.getValue();
            if (valueClass.isAssignableFrom(value.getClass())) {
                result.add(mapResult.map((V) value));
            } else if (value instanceof List) {
                List<? extends AnnotationValue> annotationValues = (List<? extends AnnotationValue>) value;
                for (AnnotationValue av : annotationValues) {
                    Object itemValue = av.getValue();
                    if (valueClass.isAssignableFrom(itemValue.getClass())) {
                        result.add(mapResult.map((V) itemValue));
                    }
                }
            }
        }
        return result;
    }
}
