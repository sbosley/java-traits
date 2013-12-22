package com.sambosley.javatraits.processor.data;

import java.util.List;

import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import com.sambosley.javatraits.annotations.HasTraits;
import com.sambosley.javatraits.utils.FullyQualifiedName;
import com.sambosley.javatraits.utils.Utils;

public class ClassWithTraits extends TypeElementWrapper {

    public static final String GEN_SUFFIX = "Gen";
    public static final String DELEGATE_SUFFIX = "Delegate";

    private List<FullyQualifiedName> traitClasses;
    private FullyQualifiedName desiredSuperclass;
    private FullyQualifiedName generatedSuperclass;

    public ClassWithTraits(TypeElement elem, Messager messager) {
        super(elem, messager);
        initTraitClasses();
        initSuperclasses();
    }

    private void initTraitClasses() {
        traitClasses = Utils.getClassFromAnnotation(HasTraits.class, elem, "traits", messager);
    }

    private void initSuperclasses() {
        List<FullyQualifiedName> desiredSuperclassResult = Utils.getClassFromAnnotation(HasTraits.class, elem, "desiredSuperclass", messager); 
        desiredSuperclass = desiredSuperclassResult.size() > 0 ? desiredSuperclassResult.get(0) : new FullyQualifiedName("java.lang.Object");
        generatedSuperclass = new FullyQualifiedName(fqn.toString() + GEN_SUFFIX);

        TypeMirror superclass = elem.getSuperclass();
        if (!generatedSuperclass.toString().equals(superclass.toString())) {
            messager.printMessage(Kind.ERROR, "This class does not have the correct superclass. It should extend " + generatedSuperclass.getSimpleName(), elem);
        }

    }

    public FullyQualifiedName getFullyQualifiedGeneratedSuperclassName() {
        return generatedSuperclass;
    }

    public List<FullyQualifiedName> getTraitClasses() {
        return traitClasses;
    }

    public FullyQualifiedName getDesiredSuperclass() {
        return desiredSuperclass;
    }

    public FullyQualifiedName getDelegateClassNameForTraitElement(TraitElement traitElement) {
        return new FullyQualifiedName(traitElement.getFullyQualifiedName() + "__" + getSimpleName() + DELEGATE_SUFFIX);
    }
}
