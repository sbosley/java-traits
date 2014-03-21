/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.data;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import com.yahoo.annotations.model.ClassName;
import com.yahoo.annotations.utils.Utils;

public class TraitElement extends TypeElementWrapper {

    public static final String INTERFACE_SUFFIX = "Interface";
    public static final String DELEGATE_SUFFIX = "DelegateWrapper";

    private List<ExecutableElement> declaredMethods;
    private ClassName interfaceName;
    private ClassName delegateName;

    public TraitElement(TypeElement elem, Utils utils) {
        super(elem, utils);
        validateElement();
    }

    private void validateElement() {
        declaredMethods = new ArrayList<ExecutableElement>();
        if (!Utils.OBJECT_CLASS_NAME.equals(elem.getSuperclass().toString())) {
            utils.getMessager().printMessage(Kind.ERROR, "Trait elements must have java.lang.Object as their superclass", elem);
        }
        List<? extends Element> enclosedElements = elem.getEnclosedElements();
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
                declaredMethods.add(exec);
            }
        }
        interfaceName = new ClassName(fqn.getPackageName(), fqn.getSimpleName() + INTERFACE_SUFFIX);
        interfaceName.setTypeArgs(getTypeParameters());

        delegateName = new ClassName(fqn.getPackageName(), fqn.getSimpleName() + DELEGATE_SUFFIX);
        delegateName.setTypeArgs(getTypeParameters());
    }

    public ClassName getInterfaceName() {
        return interfaceName;
    }

    public ClassName getDelegateName() {
        return delegateName;
    }

    public List<? extends ExecutableElement> getDeclaredMethods() {
        return declaredMethods;
    }
}
