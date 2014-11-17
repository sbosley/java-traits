/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.data;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.model.GenericName;
import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.utils.AptUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraitElement extends TypeElementWrapper {

    private static final String INTERFACE_PREFIX = "I";
    private static final String DELEGATE_SUFFIX = "DelegateWrapper";

    private List<ExecutableElement> declaredMethods = new ArrayList<ExecutableElement>();
    private List<TypeName> interfaceNames;
    private List<List<ExecutableElement>> interfaceMethods = new ArrayList<List<ExecutableElement>>();
    private List<Map<String, TypeName>> interfaceGenericNameMaps;

    private DeclaredTypeName generatedInterfaceName;
    private DeclaredTypeName delegateName;

    public TraitElement(TypeElement elem, AptUtils aptUtils) {
        super(elem, aptUtils);
        initializeElement();
    }

    private void initializeElement() {
        if (!AptUtils.OBJECT_CLASS_NAME.equals(elem.getSuperclass().toString())) {
            aptUtils.getMessager().printMessage(Kind.ERROR, "Trait elements must have java.lang.Object as their superclass", elem);
        }
        accumulateMethods(elem, declaredMethods);
        generatedInterfaceName = new DeclaredTypeName(elementName.getPackageName(), INTERFACE_PREFIX + elementName.getSimpleName());
        generatedInterfaceName.setTypeArgs(getTypeParameters());

        delegateName = new DeclaredTypeName(elementName.getPackageName(), elementName.getSimpleName() + DELEGATE_SUFFIX);
        delegateName.setTypeArgs(getTypeParameters());

        initializeInterfaces();
    }

    private void accumulateMethods(Element element, List<ExecutableElement> methods) {
        List<? extends Element> enclosedElements = element.getEnclosedElements();
        for (Element e : enclosedElements) {
            if (e.getKind() != ElementKind.METHOD || !(e instanceof ExecutableElement)) {
                if (e.getKind() == ElementKind.CONSTRUCTOR && (e instanceof ExecutableElement)) {
                    if (((ExecutableElement) e).getParameters().size() > 0) {
                        aptUtils.getMessager().printMessage(Kind.ERROR, "Trait constructors cannot have arguments", e);
                    }
                } else {
                    aptUtils.getMessager().printMessage(Kind.ERROR, "Trait elements may only declare methods or abstract methods", e);
                }
            } else {
                methods.add((ExecutableElement) e);
            }
        }
    }

    private void initializeInterfaces() {
        List<? extends TypeMirror> interfaces = elem.getInterfaces();
        if (!AptUtils.isEmpty(interfaces)) {
            initializeInterfaceMappings(interfaces);
        }
    }

    private void initializeInterfaceMappings(List<? extends TypeMirror> interfaces) {
        interfaceGenericNameMaps = new ArrayList<Map<String, TypeName>>();
        interfaceNames = aptUtils.getTypeNamesFromTypeMirrors(interfaces, getSimpleName());
        for (int i = 0; i < interfaces.size(); i++) {
            TypeMirror interfaceMirror = interfaces.get(i);
            if (interfaceMirror instanceof DeclaredType) {
                initializeSingleInterfaceMapping((DeclaredType) interfaceMirror, (DeclaredTypeName) interfaceNames.get(i));
            } else {
                aptUtils.getMessager().printMessage(Kind.WARNING, "Interface " + interfaceMirror + " from trait is not a DeclaredType", elem);
            }
        }
    }

    private void initializeSingleInterfaceMapping(DeclaredType interfaceMirror, DeclaredTypeName interfaceName) {
        TypeElement interfaceElement = (TypeElement) interfaceMirror.asElement();
        List<ExecutableElement> methods = new ArrayList<ExecutableElement>();
        accumulateMethods(interfaceElement, methods);
        interfaceMethods.add(methods);

        List<? extends TypeName> args = interfaceName.getTypeArgs();
        List<TypeName> interfaceTypeParams = aptUtils.typeParameterElementsToTypeNames(interfaceElement.getTypeParameters());
        Map<String, TypeName> genericNameMap = new HashMap<String, TypeName>();
        if (!AptUtils.isEmpty(args)) {
            for (int i = 0; i < args.size(); i++) {
                TypeName argName = args.get(i);
                TypeName interfaceArgName = interfaceTypeParams.get(i);
                if (interfaceArgName instanceof GenericName) {
                    genericNameMap.put(getSimpleName() + GenericName.GENERIC_QUALIFIER_SEPARATOR + ((GenericName) interfaceArgName).getGenericName(),
                            argName);
                }
            }
        }
        interfaceGenericNameMaps.add(genericNameMap);
    }

    public DeclaredTypeName getGeneratedInterfaceName() {
        return generatedInterfaceName;
    }

    public DeclaredTypeName getDelegateName() {
        return delegateName;
    }

    public List<ExecutableElement> getDeclaredMethods() {
        return declaredMethods;
    }

    public List<TypeName> getInterfaceNames() {
        return interfaceNames;
    }

    public int getNumSuperinterfaces() {
        return interfaceNames != null ? interfaceNames.size() : 0;
    }

    public List<ExecutableElement> getExecutableElementsForInterface(int ith) {
        return interfaceMethods.get(ith);
    }

    public Map<String, TypeName> getGenericNameMapForInterface(int ith) {
        return interfaceGenericNameMaps.get(ith);
    }

}
