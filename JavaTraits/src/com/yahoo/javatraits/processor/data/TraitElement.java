/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.data;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.utils.Utils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import java.util.ArrayList;
import java.util.List;

public class TraitElement extends TypeElementWrapper {

    private static final String INTERFACE_PREFIX = "I";
    private static final String DELEGATE_SUFFIX = "DelegateWrapper";

    private List<ExecutableElement> declaredMethods = new ArrayList<ExecutableElement>();
    private List<ExecutableElement> interfaceMethods = new ArrayList<ExecutableElement>();
    private List<TypeName> interfaceNames;

    private DeclaredTypeName generatedInterfaceName;
    private DeclaredTypeName delegateName;

    public TraitElement(TypeElement elem, Utils utils) {
        super(elem, utils);
        initializeElement();
    }

    private void initializeElement() {
        if (!Utils.OBJECT_CLASS_NAME.equals(elem.getSuperclass().toString())) {
            utils.getMessager().printMessage(Kind.ERROR, "Trait elements must have java.lang.Object as their superclass", elem);
        }
        accumulateMethods(elem, declaredMethods);
        generatedInterfaceName = new DeclaredTypeName(elementName.getPackageName(), INTERFACE_PREFIX + elementName.getSimpleName());
        generatedInterfaceName.setTypeArgs(getTypeParameters());

        delegateName = new DeclaredTypeName(elementName.getPackageName(), elementName.getSimpleName() + DELEGATE_SUFFIX);
        delegateName.setTypeArgs(getTypeParameters());

        initializeInterfaces();
    }

    private void initializeInterfaces() {
        List<? extends TypeMirror> interfaces = elem.getInterfaces();
        interfaceNames = utils.getTypeNamesFromTypeMirrors(interfaces, getSimpleName());
        for (TypeMirror i : interfaces) {
            if (i instanceof DeclaredType) {
                Element interfaceElement = ((DeclaredType) i).asElement();
                accumulateMethods(interfaceElement, interfaceMethods);
            }
        }
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

    public List<ExecutableElement> getInterfaceMethods() {
        return interfaceMethods;
    }

    public List<TypeName> getInterfaceNames() {
        return interfaceNames;
    }

    private void accumulateMethods(Element element, List<ExecutableElement> methods) {
        List<? extends Element> enclosedElements = element.getEnclosedElements();
        for (Element e : enclosedElements) {
            if (e.getKind() != ElementKind.METHOD || !(e instanceof ExecutableElement))
                if (e.getKind() == ElementKind.CONSTRUCTOR && (e instanceof ExecutableElement)) {
                    if (((ExecutableElement) e).getParameters().size() > 0) {
                        utils.getMessager().printMessage(Kind.ERROR, "Trait constructors cannot have arguments", e);
                    }
                } else {
                    utils.getMessager().printMessage(Kind.ERROR, "Trait elements may only declare methods or abstract methods", e);
                }
            else {
                ExecutableElement exec = (ExecutableElement) e;
                methods.add(exec);
            }
        }
    }

}
