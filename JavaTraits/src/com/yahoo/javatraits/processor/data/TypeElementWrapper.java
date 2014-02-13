/**
 * Copyright 2014 Yahoo Inc.
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.data;

import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import com.yahoo.annotations.ClassName;
import com.yahoo.annotations.TypeName;
import com.yahoo.annotations.Utils;

public abstract class TypeElementWrapper {

    protected TypeElement elem;
    protected Utils utils;
    protected ClassName fqn;
    protected List<TypeName> typeParameters;
    
    public TypeElementWrapper(TypeElement elem, Utils utils) {
        this.elem = elem;
        this.utils = utils;
        this.fqn = new ClassName(elem.getQualifiedName().toString());
        this.typeParameters = initTypeParameters(elem);
    }
    
    private List<TypeName> initTypeParameters(TypeElement elem) {
        List<? extends TypeParameterElement> typeParams = elem.getTypeParameters();
        return utils.mapTypeParameterElementsToTypeName(typeParams, getSimpleName());
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
