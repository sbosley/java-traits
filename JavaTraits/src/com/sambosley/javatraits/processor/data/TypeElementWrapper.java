package com.sambosley.javatraits.processor.data;

import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;

import com.sambosley.javatraits.utils.FullyQualifiedName;

public abstract class TypeElementWrapper {

    protected TypeElement elem;
    protected Messager messager;
    protected FullyQualifiedName fqn;

    public TypeElementWrapper(TypeElement elem, Messager messager) {
        this.elem = elem;
        this.messager = messager;
        this.fqn = new FullyQualifiedName(elem.getQualifiedName().toString());
    }

    public TypeElement getSourceElement() {
        return elem;
    }

    public FullyQualifiedName getFullyQualifiedName() {
        return fqn;
    }

    public String getFullyQualifiedNameAsString() {
        return fqn.toString();
    }

    public String getPackageName() {
        return fqn.getPackageName();
    }

    public String getSimpleName() {
        return fqn.getSimpleName();
    }
}
