/**
 * Copyright 2014 Yahoo Inc.
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.data;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.utils.AptUtils;

import javax.lang.model.element.TypeElement;
import java.util.List;

public abstract class TypeElementWrapper {

    protected TypeElement elem;
    protected AptUtils aptUtils;
    protected DeclaredTypeName elementName;
    protected List<TypeName> typeParameters;
    
    public TypeElementWrapper(TypeElement elem, AptUtils aptUtils) {
        this.elem = elem;
        this.aptUtils = aptUtils;
        this.elementName = new DeclaredTypeName(elem.getQualifiedName().toString());
        this.typeParameters = aptUtils.typeParameterElementsToTypeNames(elem.getTypeParameters(), getSimpleName());
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

}
