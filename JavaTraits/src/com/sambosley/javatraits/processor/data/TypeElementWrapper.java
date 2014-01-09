/**
 * Copyright 2014 Sam Bosley
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.sambosley.javatraits.processor.data;

import java.util.List;

import javax.annotation.processing.Messager;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;

import com.sambosley.javatraits.utils.FullyQualifiedName;
import com.sambosley.javatraits.utils.Utils;

public abstract class TypeElementWrapper {

    protected TypeElement elem;
    protected Messager messager;
    protected FullyQualifiedName fqn;
    protected List<? extends TypeParameterElement> typeParameters;
    
    public TypeElementWrapper(TypeElement elem, Messager messager) {
        this.elem = elem;
        this.messager = messager;
        this.fqn = new FullyQualifiedName(elem.getQualifiedName().toString());
        this.typeParameters = elem.getTypeParameters();
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
    
    public List<? extends TypeParameterElement> getTypeParameters() {
        return typeParameters;
    }
    
    public boolean hasTypeParameters() {
        return typeParameters.size() > 0;
    }
    
    public void emitParametrizedTypeList(StringBuilder builder, boolean appendBounds) {
        List<? extends TypeParameterElement> typeParams = getTypeParameters();
        for (int i = 0; i < typeParams.size(); i++) {
            TypeMirror type = typeParams.get(i).asType();
            builder.append(Utils.getSimpleTypeName(type, getSimpleName(), appendBounds));
            if (i < typeParams.size() - 1)
                builder.append(", ");
        }
    }
}
