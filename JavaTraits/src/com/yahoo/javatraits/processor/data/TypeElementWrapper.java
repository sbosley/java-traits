/**
 * Copyright 2014 Yahoo Inc.
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.data;

import java.util.List;

import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import com.yahoo.javatraits.utils.ClassName;
import com.yahoo.javatraits.utils.TypeName;
import com.yahoo.javatraits.utils.Utils;

public abstract class TypeElementWrapper {

    protected TypeElement elem;
    protected Messager messager;
    protected ClassName fqn;
    protected List<TypeName> typeParameters;
    
    public TypeElementWrapper(TypeElement elem, Messager messager) {
        this.elem = elem;
        this.messager = messager;
        this.fqn = new ClassName(elem.getQualifiedName().toString());
        this.typeParameters = initTypeParameters(elem);
    }
    
    private List<TypeName> initTypeParameters(TypeElement elem) {
        List<? extends TypeParameterElement> typeParams = elem.getTypeParameters();
        return Utils.mapTypeParameterElementsToTypeName(typeParams, getSimpleName());
    }

    public TypeElement getSourceElement() {
        return elem;
    }

    public ClassName getFullyQualifiedName() {
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
    
    public List<TypeName> getTypeParameters() {
        return typeParameters;
    }
    
    public boolean hasTypeParameters() {
        return typeParameters.size() > 0;
    }
}
