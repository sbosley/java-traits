/**
 * Copyright 2014 Sam Bosley
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.sambosley.javatraits.processor.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.tools.Diagnostic.Kind;

import com.sambosley.javatraits.utils.ClassName;
import com.sambosley.javatraits.utils.Utils;

public class TraitElement extends TypeElementWrapper {

    public static final String INTERFACE_SUFFIX = "Interface";

    private List<ExecutableElement> declaredMethods;
    private List<ExecutableElement> abstractMethods;
    private ClassName interfaceName;

    public TraitElement(TypeElement elem, Messager messager) {
        super(elem, messager);
        validateElement();
    }

    private void validateElement() {
        declaredMethods = new ArrayList<ExecutableElement>();
        abstractMethods = new ArrayList<ExecutableElement>();
        if (!Utils.OBJECT_CLASS_NAME.equals(elem.getSuperclass().toString())) {
            messager.printMessage(Kind.ERROR, "Trait elements must have java.lang.Object as their superclass", elem);
        }
        List<? extends Element> enclosedElements = elem.getEnclosedElements();
        for (Element e : enclosedElements) {
            if (e.getKind() != ElementKind.METHOD || !(e instanceof ExecutableElement))
                if (e.getKind() == ElementKind.CONSTRUCTOR && (e instanceof ExecutableElement)) {
                    if (((ExecutableElement) e).getParameters().size() > 0)
                        messager.printMessage(Kind.ERROR, "Trait constructors cannot have arguments", e);
                } else {					
                    messager.printMessage(Kind.ERROR, "Trait elements may only declare methods or abstract methods", e);
                }
            else {
                ExecutableElement exec = (ExecutableElement) e; 
                declaredMethods.add(exec);
                Set<Modifier> modifiers = exec.getModifiers();
                if (modifiers.contains(Modifier.ABSTRACT))
                    abstractMethods.add(exec);
            }
        }
        interfaceName = new ClassName(fqn.getPackageName(), fqn.getSimpleName() + INTERFACE_SUFFIX);
    }

    public ClassName getInterfaceName() {
        return interfaceName;
    }

    public List<? extends ExecutableElement> getDeclaredMethods() {
        return declaredMethods;
    }

    public List<? extends ExecutableElement> getAbstractMethods() {
        return abstractMethods;
    }
    
    @Deprecated
    public void emitParametrizedInterfaceName(StringBuilder builder, boolean appendBounds) {
        builder.append(getInterfaceName().getSimpleName());
        List<? extends TypeParameterElement> typeParams = elem.getTypeParameters();
        if (typeParams.size() > 0) {
            builder.append("<");
            emitParametrizedTypeList(builder, appendBounds);
            builder.append(">");
        }
    }

}
