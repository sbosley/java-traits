/**
 * Copyright 2014 Yahoo Inc.
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.data;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.utils.Utils;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import java.util.List;

public abstract class TypeElementWrapper {

    protected TypeElement elem;
    protected Utils utils;
    protected DeclaredTypeName elementName;
    protected List<TypeName> typeParameters;
    
    public TypeElementWrapper(TypeElement elem, Utils utils) {
        this.elem = elem;
        this.utils = utils;
        this.elementName = new DeclaredTypeName(elem.getQualifiedName().toString());
        this.typeParameters = initTypeParameters(elem);
    }
    
    private List<TypeName> initTypeParameters(TypeElement elem) {
        List<? extends TypeParameterElement> typeParams = elem.getTypeParameters();
        return utils.typeParameterElementsToTypeNames(typeParams, getSimpleName());
    }

    public TypeElement getSourceElement() {
        return elem;
    }

    public DeclaredTypeName getElementName() {
        return elementName;
    }

    public String getPackageName() {
        return elementName.getPackageName();
    }

    public String getSimpleName() {
        return elementName.getSimpleName();
    }
    
    public List<TypeName> getTypeParameters() {
        return typeParameters;
    }
    
    public boolean hasTypeParameters() {
        return typeParameters.size() > 0;
    }
}
