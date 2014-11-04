/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.annotations.utils;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.model.GenericName;
import com.yahoo.annotations.model.MethodSignature;
import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.model.TypeName.TypeNameVisitor;
import com.yahoo.annotations.visitors.ImportGatheringTypeMirrorVisitor;
import com.yahoo.annotations.visitors.ImportGatheringTypeNameVisitor;
import com.yahoo.annotations.writer.parameters.MethodDeclarationParameters;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.Map.Entry;

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

    public void accumulateImportsFromElements(Set<DeclaredTypeName> accumulate, Collection<? extends Element> elems) {
        if (!isEmpty(elems)) {
            for (Element elem : elems) {
                elem.asType().accept(new ImportGatheringTypeMirrorVisitor(elem, messager, this), accumulate);
            }
        }
    }

    public void accumulateImportsFromTypeNames(Set<DeclaredTypeName> accumulate, Collection<? extends TypeName> typeNames) {
        if (!isEmpty(typeNames)) {
            ImportGatheringTypeNameVisitor visitor = new ImportGatheringTypeNameVisitor();
            for (TypeName typeName : typeNames) {
                typeName.accept(visitor, accumulate);
            }
        }
    }

    public MethodSignature executableElementToMethodSignature(ExecutableElement exec) {
        return executableElementToMethodSignature(exec, null);
    }
    
    public MethodSignature executableElementToMethodSignature(ExecutableElement exec, String genericQualifier) {
        String name = exec.getSimpleName().toString();
        MethodSignature result = new MethodSignature(name);

        List<TypeName> methodGenerics = typeParameterElementsToTypeNames(exec.getTypeParameters());
        TypeName returnType = getTypeNameFromTypeMirror(exec.getReturnType());
        qualifyTypeArgGenerics(returnType, methodGenerics, genericQualifier);
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

    public List<TypeName> getTypeNamesFromTypeMirrors(List<? extends TypeMirror> mirrors) {
        return getTypeNamesFromTypeMirrors(mirrors, null);
    }

    public List<TypeName> getTypeNamesFromTypeMirrors(List<? extends TypeMirror> mirrors, final String genericQualifier) {
        return map(mirrors, new Mapper<TypeMirror, TypeName>() {
            @Override
            public TypeName map(TypeMirror arg) {
                return getTypeNameFromTypeMirror(arg, genericQualifier);
            }
        });
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
        GenericName toReturn = new GenericName(genericName, extendsBound, superBound);
        toReturn.addQualifier(genericQualifier);
        return toReturn;
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
        List<TypeName> methodGenerics = typeParameterElementsToTypeNames(exec.getTypeParameters());
        TypeName returnType = getTypeNameFromTypeMirror(exec.getReturnType());
        qualifyTypeArgGenerics(returnType, methodGenerics, genericQualifier);

        Pair<List<TypeName>, List<String>> arguments = getMethodArgumentsFromExecutableElement(exec, genericQualifier, methodGenerics);

        return new MethodDeclarationParameters()
            .setMethodName(name)
            .setReturnType(returnType)
            .setModifiers(modifiers)
            .setMethodGenerics(methodGenerics)
            .setArgumentTypes(arguments.getLeft())
            .setArgumentNames(arguments.getRight())
            .setThrowsTypes(getThrownTypes(exec, genericQualifier, methodGenerics));
    }

    public List<? extends TypeName> remapGenericNames(List<? extends TypeName> types, final Map<String, Object> genericNameMap) {
        if (Utils.isEmpty(genericNameMap)) {
            return types;
        }
        return map(types, new Mapper<TypeName, TypeName>() {
            @Override
            public TypeName map(TypeName arg) {
                return remapGenericNames(arg, genericNameMap);
            }
        });
    }

    public TypeName remapGenericNames(TypeName type, Map<String, Object> genericNameMap) {
        if (type != null && genericNameMap != null) {
            return type.accept(genericNameRemappingVisitor, genericNameMap);
        }
        return type;
    }

    private TypeNameVisitor<TypeName, Map<String, Object>> genericNameRemappingVisitor = new TypeNameVisitor<TypeName, Map<String, Object>>() {
        @Override
        public TypeName visitClassName(DeclaredTypeName typeName, Map<String, Object> genericNameMap) {
            typeName.setTypeArgs(remapGenericNames(typeName.getTypeArgs(), genericNameMap));
            return typeName;
        }

        @Override
        public TypeName visitGenericName(GenericName genericName, Map<String, Object> genericNameMap) {
            String genericNameString = genericName.getGenericName();
            if (genericNameMap.containsKey(genericNameString)) {
                Object renameTo = genericNameMap.get(genericNameString);
                if (renameTo instanceof TypeName) {
                    return (TypeName) renameTo;
                } else if (renameTo instanceof String) {
                    genericName.renameTo((String) renameTo);
                }
            }

            genericName.setExtendsBound(remapGenericNames(genericName.getExtendsBound(), genericNameMap));
            genericName.setSuperBound(remapGenericNames(genericName.getSuperBound(), genericNameMap));

            return genericName;
        }
    };

    private void qualifyTypeArgGenerics(TypeName toQualify, List<TypeName> methodGenerics, String genericQualifier) {
        qualifyTypeArgGenerics(toQualify, Pair.create(methodGenerics, genericQualifier));
    }

    private void qualifyTypeArgGenerics(TypeName toQualify, Pair<List<TypeName>, String> params) {
        if (toQualify != null) {
            toQualify.accept(genericQualifyingVisitor, params);
        }
    }

    private void qualifyTypeArgGenerics(List<? extends TypeName> toQualify, List<TypeName> methodGenerics, String genericQualifier) {
        qualifyTypeArgGenerics(toQualify, Pair.create(methodGenerics, genericQualifier));
    }

    private void qualifyTypeArgGenerics(List<? extends TypeName> toQualify, final Pair<List<TypeName>, String> params) {
        foreach(toQualify, new ForEachMapper<TypeName>() {
            @Override
            public void apply(TypeName arg) {
                qualifyTypeArgGenerics(arg, params);
            }
        });
    }

    private TypeNameVisitor genericQualifyingVisitor = new TypeNameVisitor<Void, Pair<List<TypeName>, String>>() {

        @Override
        public Void visitClassName(DeclaredTypeName typeName, Pair<List<TypeName>, String> params) {
            qualifyTypeArgGenerics(typeName.getTypeArgs(), params);
            return null;
        }

        @Override
        public Void visitGenericName(GenericName genericName, Pair<List<TypeName>, String> params) {
            if (params.getLeft() != null && !params.getLeft().contains(genericName)) {
                genericName.addQualifier(params.getRight());
            }
            qualifyTypeArgGenerics(genericName.getExtendsBound(), params);
            qualifyTypeArgGenerics(genericName.getSuperBound(), params);
            return null;
        }
    };

    private List<TypeName> getArgumentTypeNames(ExecutableElement exec, final String genericQualifier, final List<TypeName> methodGenerics) {
        List<TypeName> typeNames = map(exec.getParameters(), new Mapper<VariableElement, TypeName>() {
            @Override
            public TypeName map(VariableElement arg) {
                return getTypeNameFromTypeMirror(arg.asType());
            }
        });

        qualifyTypeArgGenerics(typeNames, methodGenerics, genericQualifier);
        return typeNames;
    }

    private List<String> getArgumentNames(ExecutableElement exec) {
        return map(exec.getParameters(), new Mapper<VariableElement, String>() {
            @Override
            public String map(VariableElement arg) {
                return arg.toString();
            }
        });
    }

    private Pair<List<TypeName>, List<String>> getMethodArgumentsFromExecutableElement(ExecutableElement exec, final String genericQualifier, final List<TypeName> methodGenerics) {
        List<TypeName> typeNames = getArgumentTypeNames(exec, genericQualifier, methodGenerics);
        List<String> args = getArgumentNames(exec);
        if (exec.isVarArgs()) {
            typeNames.get(typeNames.size() - 1).setIsVarArgs(true);
        }
        return Pair.create(typeNames, args);
    }

    private List<TypeName> getThrownTypes(ExecutableElement exec, final String genericQualifier, final List<TypeName> methodGenerics) {
        List<TypeName> thrownTypes = map(exec.getThrownTypes(), new Utils.Mapper<TypeMirror, TypeName>() {
            @Override
            public TypeName map(TypeMirror arg) {
                return getTypeNameFromTypeMirror(arg);
            }
        });
        qualifyTypeArgGenerics(thrownTypes, methodGenerics, genericQualifier);
        return thrownTypes;
    }

    // --- static methods
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
    
    public static <T> List<T> asList(T... args) {
        return args == null ? null : Arrays.asList(args);
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
        if (list == null) {
            return null;
        }
        List<B> result = new ArrayList<B>();
        for (A elem : list) {
            result.add(mapFunction.map(elem));
        }
        return result;
    }

    public static interface ForEachMapper<A> {
        public void apply(A arg);
    }

    public static <A> void foreach(List<? extends A> list, ForEachMapper<A> function) {
        if (list == null) {
            return;
        }
        for (A elem : list) {
            function.apply(elem);
        }
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
        return getTypeNamesFromAnnotationValue(annotationValue);
    }
    
    public List<TypeMirror> getClassMirrorsFromAnnotation(Element elem, Class<?> annotationClass, String propertyName) {
        AnnotationValue annotationValue = getAnnotationValue(elem, annotationClass, propertyName);
        return getTypeMirrorsFromAnnotationValue(annotationValue);
    }

    public List<DeclaredTypeName> getTypeNamesFromAnnotationValue(AnnotationValue annotationValue) {
        return mapValuesFromAnnotationValue(annotationValue, TypeMirror.class, new Mapper<TypeMirror, DeclaredTypeName>() {
            @Override
            public DeclaredTypeName map(TypeMirror arg) {
                return new DeclaredTypeName(arg.toString());
            }
        });
    }
    
    public List<TypeMirror> getTypeMirrorsFromAnnotationValue(AnnotationValue annotationValue) {
        return getValuesFromAnnotationValue(annotationValue, TypeMirror.class);
    }
    
    public <T> List<T> getValuesFromAnnotationValue(AnnotationValue annotationValue, Class<T> valueClass) {
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
