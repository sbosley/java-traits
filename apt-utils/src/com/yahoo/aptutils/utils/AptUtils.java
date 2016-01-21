/*
 * Copyright 2014 Yahoo Inc.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yahoo.aptutils.utils;

import com.yahoo.aptutils.model.*;
import com.yahoo.aptutils.model.TypeName.TypeNameVisitor;
import com.yahoo.aptutils.visitors.ImportGatheringTypeMirrorVisitor;
import com.yahoo.aptutils.visitors.ImportGatheringTypeNameVisitor;
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Types;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Utilities class containing functions that facilitate working with the apt-utils library.
 * Contains several helper methods for converting the classes used by the Java APT to more basic
 * objects used by apt-utils.
 */
public class AptUtils {

    /**
     * "java.lang.Object"
     */
    public static final String OBJECT_CLASS_NAME = CoreTypes.JAVA_OBJECT.toString();

    private final ProcessingEnvironment env;
    private final Messager messager;
    private final Types types;
    private final Elements elements;
    private final Filer filer;

    /**
     * @param env a {@link ProcessingEnvironment}
     */
    public AptUtils(ProcessingEnvironment env) {
        this.env = env;
        this.messager = env.getMessager();
        this.types = env.getTypeUtils();
        this.elements = env.getElementUtils();
        this.filer = env.getFiler();
    }

    /**
     * @return the {@link ProcessingEnvironment} this AptUtils was constructed from
     */
    public ProcessingEnvironment getProcessingEnvironment() {
        return env;
    }

    /**
     * @return an instance of {@link Messager} for the ProcessingEnvironment this AptUtils was constructed with
     */
    public Messager getMessager() {
        return messager;
    }

    /**
     * @return an instance of {@link Types} for the ProcessingEnvironment this AptUtils was constructed with
     */
    public Types getTypes() {
        return types;
    }

    /**
     * @return an instance of {@link Elements} for the ProcessingEnvironment this AptUtils was constructed with
     */
    public Elements getElements() {
        return elements;
    }

    /**
     * @return an instance of {@link Filer} for the ProcessingEnvironment this AptUtils was constructed with
     */
    public Filer getFiler() {
        return filer;
    }

    /**
     *
     * @param generatedTypeName the fully qualified type name for the file to be generated
     * @param sourceElement the {@link Element} that caused this file to be generated
     * @return a new instance of {@link JavaFileWriter} for the given type
     * @throws IOException
     */
    public JavaFileWriter newJavaFileWriter(DeclaredTypeName generatedTypeName, Element sourceElement) throws IOException {
        JavaFileObject jfo = filer.createSourceFile(generatedTypeName.toString(), sourceElement);
        return new JavaFileWriter(jfo.openWriter());
    }

    // --- Imports helpers

    /**
     * @param accumulate a {@link Set} in which to add accumulated imports required by the given elements
     * @param elems {@link Element}s to accumulate imports from
     */
    public void accumulateImportsFromElements(Set<DeclaredTypeName> accumulate, Collection<? extends Element> elems) {
        if (!isEmpty(elems)) {
            for (Element elem : elems) {
                elem.asType().accept(new ImportGatheringTypeMirrorVisitor(elem, this), accumulate);
            }
        }
    }

    /**
     * @param accumulate a {@link Set} in which to add accumulated imports required by the given TypeNames
     * @param typeNames {@link TypeName}s to accumulate imports from
     */
    public void accumulateImportsFromTypeNames(Set<DeclaredTypeName> accumulate, Collection<? extends TypeName> typeNames) {
        if (!isEmpty(typeNames)) {
            ImportGatheringTypeNameVisitor visitor = new ImportGatheringTypeNameVisitor();
            for (TypeName typeName : typeNames) {
                typeName.accept(visitor, accumulate);
            }
        }
    }

    // --- Method signature helpers

    /**
     * @param exec the {@link ExecutableElement} to convert
     * @return a {@link MethodSignature} constructed from the given {@link ExecutableElement}
     */
    public MethodSignature executableElementToMethodSignature(ExecutableElement exec) {
        return executableElementToMethodSignature(exec, null);
    }

    /**
     * @param exec the {@link ExecutableElement} to convert
     * @param genericQualifier optional string to qualify any generics found in the ExecutableElement.
     *                         E.g. with qualifier "Q", generic name "T" would be replaced by "Q_T"
     * @return a {@link MethodSignature} constructed from the given
     * {@link ExecutableElement}
     */
    public MethodSignature executableElementToMethodSignature(ExecutableElement exec, String genericQualifier) {
        String name = exec.getSimpleName().toString();
        MethodSignature result = new MethodSignature(name);

        List<TypeName> methodGenerics = typeParameterElementsToTypeNames(exec.getTypeParameters());
        TypeName returnType = getTypeNameFromTypeMirror(exec.getReturnType());
        qualifyTypeArgGenerics(returnType, methodGenerics, genericQualifier);
        result.setReturnType(returnType);

        List<TypeName> argTypeNames = getArgumentTypeNames(exec, genericQualifier, methodGenerics);
        result.addArgTypes(argTypeNames);

        List<TypeName> throwsTypeNames = getThrownTypes(exec, genericQualifier, methodGenerics);
        result.addThrowsTypes(throwsTypeNames);
        return result;
    }

    // --- TypeName creation methods

    /**
     * @return a {@link TypeName} corresponding to the given
     *          {@link TypeParameterElement}
     */
    public TypeName typeParameterElementToTypeName(TypeParameterElement elem) {
        return typeParameterElementToTypeName(elem, null);
    }

    /**
     * @param elem the {@link TypeParameterElement} to convert
     * @param genericQualifier optional string to qualify the generic name found in the TypeParameterElement.
     *                         E.g. with qualifier "Q", generic name "T" would be replaced by "Q_T"
     * @return a {@link TypeName} corresponding to the given {@link TypeParameterElement}
     */
    public TypeName typeParameterElementToTypeName(TypeParameterElement elem, String genericQualifier) {
        return getTypeNameFromTypeMirror(elem.asType(), genericQualifier);
    }

    /**
     * @return a {@link List}&lt;{@link TypeName}&gt; converted from the given {@link TypeParameterElement}s. Equivalent
     * to calling {@link #typeParameterElementToTypeName(TypeParameterElement)} on each item in the list.
     */
    public <T extends TypeParameterElement> List<TypeName> typeParameterElementsToTypeNames(List<T> params) {
        return typeParameterElementsToTypeNames(params, null);
    }

    /**
     * @return a {@link List}&lt;{@link TypeName}&gt; converted from the given {@link TypeParameterElement}s. Equivalent
     * to calling {@link #typeParameterElementToTypeName(TypeParameterElement, String)} on each item in the list.
     */
    public <T extends TypeParameterElement> List<TypeName> typeParameterElementsToTypeNames(List<T> params, final String genericQualifier) {
        return map(params, new Function<T, TypeName>() {
            @Override
            public TypeName map(TypeParameterElement arg) {
                return typeParameterElementToTypeName(arg, genericQualifier);
            }
        });
    }

    /**
     * @return a {@link TypeName} representing the given {@link TypeMirror}
     */
    public TypeName getTypeNameFromTypeMirror(TypeMirror mirror) {
        return getTypeNameFromTypeMirror(mirror, null);
    }

    /**
     * @param mirror the {@link TypeMirror} to convert
     * @param genericQualifier optional string to qualify the generic name found in the TypeParameterElement.
     *                         E.g. with qualifier "Q", generic name "T" would be replaced by "Q_T"
     * @return a {@link TypeName} representing the given {@link TypeMirror}
     */
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
        if (kind == TypeKind.VOID) {
            return CoreTypes.VOID;
        } else if (kind == TypeKind.TYPEVAR) {
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
                    typeArgs = map(declaredTypeArgs, new Function<TypeMirror, TypeName>() {
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

    /**
     * @return a {@link List}&lt;{@link TypeName}&gt; converted from the given {@link TypeMirror}s. Equivalent to
     * calling {@link #getTypeNameFromTypeMirror(TypeMirror)} on each item in the list.
     */
    public List<TypeName> getTypeNamesFromTypeMirrors(List<? extends TypeMirror> mirrors) {
        return getTypeNamesFromTypeMirrors(mirrors, null);
    }

    /**
     * @return a {@link List}&lt;{@link TypeName}&gt; converted from the given {@link TypeMirror}s. Equivalent to
     * calling {@link #getTypeNameFromTypeMirror(TypeMirror, String)} on each item in the list.
     */
    public List<TypeName> getTypeNamesFromTypeMirrors(List<? extends TypeMirror> mirrors, final String genericQualifier) {
        return map(mirrors, new Function<TypeMirror, TypeName>() {
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
        toReturn.setQualifier(genericQualifier);
        return toReturn;
    }

    private List<TypeName> getUpperBoundsFromTypeMirror(TypeMirror sourceMirror, TypeMirror extendsBoundMirror, final String genericQualifier) {
        List<? extends TypeMirror> upperBounds = getUpperBoundMirrors(sourceMirror, extendsBoundMirror);
        return map(upperBounds, new Function<TypeMirror, TypeName>() {
            @Override
            public TypeName map(TypeMirror arg) {
                return getTypeNameFromTypeMirror(arg, genericQualifier);
            }
        });
    }

    /**
     * @param sourceMirror the {@link TypeMirror} to get upper bound mirrors from
     * @param extendsBoundMirror the extends bound of the sourceMirror. Should usually be obtained by calling
     *                           {@link TypeVariable#getUpperBound()} or {@link WildcardType#getExtendsBound()}
     * @return a {@link List}&lt;{@link TypeMirror}&gt; representing the upper bounds of the source mirror. Will only
     * contain more than one element if the upper bound is an intersection type
     */
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

    // --- MethodDeclarationParameters helpers

    /**
     * @param exec the {@link ExecutableElement} to convert to {@link MethodDeclarationParameters}
     * @param modifiers the desired modifiers for the new method declaration
     * @return a {@link MethodDeclarationParameters} suitable as an argument to
     * {@link JavaFileWriter#beginMethodDefinition(MethodDeclarationParameters)}
     */
    public MethodDeclarationParameters methodDeclarationParamsFromExecutableElement(ExecutableElement exec, Modifier... modifiers) {
        return methodDeclarationParamsFromExecutableElement(exec, null, modifiers);
    }

    /**
     * @param exec the {@link ExecutableElement} to convert to {@link MethodDeclarationParameters}
     * @param nameOverride the desired name for the new method, or null if the name from exec should be used
     * @param modifiers the desired modifiers for the new method declaration
     * @return a {@link MethodDeclarationParameters} suitable as an argument to
     * {@link JavaFileWriter#beginMethodDefinition(MethodDeclarationParameters)}
     */
    public MethodDeclarationParameters methodDeclarationParamsFromExecutableElement(ExecutableElement exec, String nameOverride, Modifier... modifiers) {
        return methodDeclarationParamsFromExecutableElement(exec, nameOverride, null, modifiers);
    }

    /**
     * @param exec the {@link ExecutableElement} to convert to {@link MethodDeclarationParameters}
     * @param nameOverride the desired name for the new method, or null if the name from exec should be used
     * @param genericQualifier optional string to qualify the generic name found in the TypeParameterElement.
      *                         E.g. with qualifier "Q", generic name "T" would be replaced by "Q_T"
     * @param modifiers the desired modifiers for the new method declaration
     * @return a {@link MethodDeclarationParameters} suitable as an argument to
     * {@link JavaFileWriter#beginMethodDefinition(MethodDeclarationParameters)}
     */
    public MethodDeclarationParameters methodDeclarationParamsFromExecutableElement(ExecutableElement exec, String nameOverride,
            String genericQualifier, Modifier... modifiers) {
        String name = nameOverride != null ? nameOverride : exec.getSimpleName().toString();

        List<TypeName> methodGenerics = typeParameterElementsToTypeNames(exec.getTypeParameters());
        Pair<List<TypeName>, List<String>> arguments = getMethodArgumentsFromExecutableElement(exec, genericQualifier, methodGenerics);

        return new MethodDeclarationParameters()
            .setMethodName(name)
            .setReturnType(getReturnTypeName(exec, genericQualifier, methodGenerics))
            .setModifiers(modifiers)
            .setMethodGenerics(methodGenerics)
            .setArgumentTypes(arguments.getLeft())
            .setArgumentNames(arguments.getRight())
            .setThrowsTypes(getThrownTypes(exec, genericQualifier, methodGenerics));
    }

    private TypeName getReturnTypeName(ExecutableElement exec, String genericQualifier, List<TypeName> methodGenerics) {
        TypeName returnType = getTypeNameFromTypeMirror(exec.getReturnType());
        qualifyTypeArgGenerics(returnType, methodGenerics, genericQualifier);
        return returnType;
    }

    private Pair<List<TypeName>, List<String>> getMethodArgumentsFromExecutableElement(ExecutableElement exec, final String genericQualifier, final List<TypeName> methodGenerics) {
        List<TypeName> typeNames = getArgumentTypeNames(exec, genericQualifier, methodGenerics);
        List<String> args = getArgumentNames(exec);
        if (exec.isVarArgs()) {
            typeNames.get(typeNames.size() - 1).setIsVarArgs(true);
        }
        return Pair.create(typeNames, args);
    }

    private List<TypeName> getArgumentTypeNames(ExecutableElement exec, final String genericQualifier, final List<TypeName> methodGenerics) {
        List<TypeName> typeNames = map(exec.getParameters(), new Function<VariableElement, TypeName>() {
            @Override
            public TypeName map(VariableElement arg) {
                return getTypeNameFromTypeMirror(arg.asType());
            }
        });

        qualifyTypeArgGenerics(typeNames, methodGenerics, genericQualifier);
        return typeNames;
    }

    private List<String> getArgumentNames(ExecutableElement exec) {
        return map(exec.getParameters(), new Function<VariableElement, String>() {
            @Override
            public String map(VariableElement arg) {
                return arg.toString();
            }
        });
    }

    private List<TypeName> getThrownTypes(ExecutableElement exec, final String genericQualifier, final List<TypeName> methodGenerics) {
        List<TypeName> thrownTypes = map(exec.getThrownTypes(), new Function<TypeMirror, TypeName>() {
            @Override
            public TypeName map(TypeMirror arg) {
                return getTypeNameFromTypeMirror(arg);
            }
        });
        qualifyTypeArgGenerics(thrownTypes, methodGenerics, genericQualifier);
        return thrownTypes;
    }

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
        if (isEmpty(toQualify)) {
            return;
        }
        for (TypeName item : toQualify) {
            qualifyTypeArgGenerics(item, params);
        }
    }

    private TypeNameVisitor<Void, Pair<List<TypeName>, String>> genericQualifyingVisitor = new TypeNameVisitor<Void, Pair<List<TypeName>, String>>() {

        @Override
        public Void visitClassName(DeclaredTypeName typeName, Pair<List<TypeName>, String> params) {
            qualifyTypeArgGenerics(typeName.getTypeArgs(), params);
            return null;
        }

        @Override
        public Void visitGenericName(GenericName genericName, Pair<List<TypeName>, String> params) {
            if (params.getLeft() != null && !params.getLeft().contains(genericName)) {
                genericName.setQualifier(params.getRight());
            }
            qualifyTypeArgGenerics(genericName.getExtendsBound(), params);
            qualifyTypeArgGenerics(genericName.getSuperBound(), params);
            return null;
        }
    };

    // --- GenericName remapping functions

    /**
     * @param types {@link TypeName}s to remap
     * @param genericNameMap map specifying which names should be remapped to a different {@link TypeName}
     * @return a list of remapped type names
     *
     * Note: This method shouldn't need to be used very often. Sometimes it comes up that the generic name used at
     * compile time is different than the one used in code. For example, List&lt;T&gt; in your source code might be
     * represented as List&lt;E&gt; at compile time. This method makes it easy to replace references to generic names
     * with the names you expect.
     */
    public List<? extends TypeName> remapGenericNames(List<? extends TypeName> types, final Map<String, TypeName> genericNameMap) {
        if (AptUtils.isEmpty(genericNameMap)) {
            return types;
        }
        return map(types, new Function<TypeName, TypeName>() {
            @Override
            public TypeName map(TypeName arg) {
                return remapGenericNames(arg, genericNameMap);
            }
        });
    }

    /**
     * @param type {@link TypeName} to remap
     * @param genericNameMap map specifying which names should be remapped to a different {@link TypeName}
     * @return a list of remapped type names
     *
     * Note: This method shouldn't need to be used very often. Sometimes it comes up that the generic name used at
     * compile time is different than the one used in code. For example, List&lt;T&gt; in your source code might be
     * represented as List&lt;E&gt; at compile time. This method makes it easy to replace references to generic names
     * with the names you expect.
     */
    public TypeName remapGenericNames(TypeName type, Map<String, TypeName> genericNameMap) {
        if (type != null && genericNameMap != null) {
            return type.accept(genericNameRemappingVisitor, genericNameMap);
        }
        return type;
    }

    private TypeNameVisitor<TypeName, Map<String, TypeName>> genericNameRemappingVisitor = new TypeNameVisitor<TypeName, Map<String, TypeName>>() {
        @Override
        public TypeName visitClassName(DeclaredTypeName typeName, Map<String, TypeName> genericNameMap) {
            typeName.setTypeArgs(remapGenericNames(typeName.getTypeArgs(), genericNameMap));
            return typeName;
        }

        @Override
        public TypeName visitGenericName(GenericName genericName, Map<String, TypeName> genericNameMap) {
            String genericNameString = genericName.getGenericName();
            if (genericNameMap.containsKey(genericNameString)) {
                return genericNameMap.get(genericNameString);
            }

            genericName.setExtendsBound(remapGenericNames(genericName.getExtendsBound(), genericNameMap));
            genericName.setSuperBound(remapGenericNames(genericName.getSuperBound(), genericNameMap));
            return genericName;
        }
    };

    // --- AnnotationMirror and AnnotationValue helpers

    /**
     * Utility method to extract an {@link AnnotationValue} from a given element. Useful for when the value of an
     * annotation isn't a primitive type (e.g a class or another annotation)
     */
    public AnnotationValue getAnnotationValue(Element elem, Class<?> annotationClass, String propertyName) {
        AnnotationMirror mirror = getAnnotationMirror(elem, annotationClass);
        return getAnnotationValueFromMirror(mirror, propertyName);
    }

    /**
     * Utility method to read the {@link AnnotationMirror} from a given element.
     */
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

    /**
     * Utility method to get the {@link AnnotationValue} from an {@link AnnotationMirror} by property name.
     */
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

    /**
     * Reads class values from an annotation and converts them to {@link DeclaredTypeName}s
     */
    public List<DeclaredTypeName> getClassValuesFromAnnotation(Element elem, Class<?> annotationClass, String propertyName) {
        AnnotationValue annotationValue = getAnnotationValue(elem, annotationClass, propertyName);
        return getTypeNamesFromAnnotationValue(annotationValue);
    }

    /**
     * Reads class values from an annotation as their corresponding {@link TypeMirror}s
     */
    public List<TypeMirror> getClassMirrorsFromAnnotation(Element elem, Class<?> annotationClass, String propertyName) {
        AnnotationValue annotationValue = getAnnotationValue(elem, annotationClass, propertyName);
        return getTypeMirrorsFromAnnotationValue(annotationValue);
    }

    /**
     * Reads class values from an {@link AnnotationValue} and converts them to {@link DeclaredTypeName}s
     */
    public List<DeclaredTypeName> getTypeNamesFromAnnotationValue(AnnotationValue annotationValue) {
        return mapValuesFromAnnotationValue(annotationValue, TypeMirror.class, new Function<TypeMirror, DeclaredTypeName>() {
            @Override
            public DeclaredTypeName map(TypeMirror arg) {
                return new DeclaredTypeName(arg.toString());
            }
        });
    }

    /**
     * Reads class values from an {@link AnnotationValue} as their corresponding {@link TypeMirror}s
     */
    public List<TypeMirror> getTypeMirrorsFromAnnotationValue(AnnotationValue annotationValue) {
        return getValuesFromAnnotationValue(annotationValue, TypeMirror.class);
    }

    /**
     * Read arbitrary values as their native compile-time types from an {@link AnnotationValue}
     */
    public <T> List<T> getValuesFromAnnotationValue(AnnotationValue annotationValue, Class<T> valueClass) {
        return mapValuesFromAnnotationValue(annotationValue, valueClass, new Function<T, T>() {
            @Override
            public T map(T arg) {
                return arg;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <V, T> List<T> mapValuesFromAnnotationValue(AnnotationValue annotationValue, Class<V> valueClass, Function<V, T> mapResult) {
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

    // --- static methods

    /**
     * Null-safe equality checking
     * @return true if o1.equals(o2)
     */
    public static boolean isEqual(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            return o1 == o2;
        }
        return o1.equals(o2);
    }

    /**
     * Null-safe emptiness checking
     * @return true if str is null or empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Null-safe emptiness checking
     * @return true if collection is null or empty
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Null-safe emptiness checking
     * @return true if collection is map or empty
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Null-safe version of {@link java.util.Arrays#asList(Object[])}
     * @return null if args is null or {@link java.util.Arrays#asList(Object[])} if not
     */
    public static <T> List<T> asList(T... args) {
        return args == null ? null : Arrays.asList(args);
    }

    /**
     * @return the package name from a String representation of a fully qualified class name
     */
    public static String getPackageFromFullyQualifiedName(String name) {
        int split = getFQNSplitIndex(name);
        if (split < 0) {
            return "";
        }
        return name.substring(0, split);
    }

    /**
     * @return the simple class name from a String representation of a fully qualified class name
     */
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

    /**
     * Mapping function used by {@link #map(List, AptUtils.Function)}
     */
    public interface Function<A, B> {
        B map(A arg);
    }

    /**
     * @param list list to map
     * @param mapFunction mapping function to apply to each element in the source list
     * @return a new list where each element is the result of applying mapFunction to the corresponding element in
     * the source list
     */
    public static <A, B> List<B> map(List<? extends A> list, Function<A, B> mapFunction) {
        if (list == null) {
            return null;
        }
        List<B> result = new ArrayList<B>();
        for (A elem : list) {
            result.add(mapFunction.map(elem));
        }
        return result;
    }

    /**
     * Equivalent to calling {@link #deepCompareTypes(TypeName, TypeName)} on each pair corresponding elements from the
     * source lists
     */
    public static boolean deepCompareTypeList(List<? extends TypeName> l1, List<? extends TypeName> l2) {
        if (l1 == null || l2 == null) {
            return l1 == l2;
        }
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

    /**
     * @return true if:
     * <ol>
     *     <li>the two types have the same fully qualified name</li>
     *     <li>the two types have the same array depth</li>
     *     <li>the two types have the same type arguments, as evaluated by {@link #deepCompareTypeList(List, List)}</li>
     * </ol>
     */
    public static boolean deepCompareTypes(TypeName t1, TypeName t2) {
        if (t1 == null || t2 == null) {
            return t1 == t2;
        }
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
}
