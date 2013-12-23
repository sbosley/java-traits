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

public class TraitElement extends TypeElementWrapper {

    public static final String INTERFACE_SUFFIX = "Interface";

    private List<ExecutableElement> declaredMethods;
    private List<ExecutableElement> abstractMethods;

    public TraitElement(TypeElement elem, Messager messager) {
        super(elem, messager);
        validateElement();
    }

    private void validateElement() {
        declaredMethods = new ArrayList<ExecutableElement>();
        abstractMethods = new ArrayList<ExecutableElement>();
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
    }

    public String getFullyQualifiedInterfaceName() {
        return fqn.toString() + INTERFACE_SUFFIX;
    }

    public String getSimpleInterfaceName() {
        return getSimpleName() + INTERFACE_SUFFIX;
    }

    public List<? extends ExecutableElement> getDeclaredMethods() {
        return declaredMethods;
    }

    public List<? extends ExecutableElement> getAbstractMethods() {
        return abstractMethods;
    }
    
    public void emitParametrizedInterfaceName(StringBuilder builder) {
        builder.append(getSimpleInterfaceName());
        List<? extends TypeParameterElement> typeParams = getTypeParameters();
        if (typeParams.size() > 0) {
            builder.append("<");
            emitParametrizedTypeList(builder);
            builder.append(">");
        }
    }

}
