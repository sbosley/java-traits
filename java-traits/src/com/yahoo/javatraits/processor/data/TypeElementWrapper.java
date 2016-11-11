/*
 * Copyright 2014 Yahoo Inc.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yahoo.javatraits.processor.data;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.utils.AptUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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
        Element pack=elem.getEnclosingElement();
        String prefix="";
        while(pack.getKind()!=ElementKind.PACKAGE){
            prefix+=pack.getSimpleName().toString()+"_";
            pack=pack.getEnclosingElement();
        }
        this.elementName = new DeclaredTypeName(pack.toString(),prefix+elem.getSimpleName().toString());
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
