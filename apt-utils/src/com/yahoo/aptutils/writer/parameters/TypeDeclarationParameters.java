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
package com.yahoo.aptutils.writer.parameters;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * An object for containing the information needed to begin a type declaration. Required by
 * {@link com.yahoo.aptutils.writer.JavaFileWriter#beginTypeDefinition(TypeDeclarationParameters)}
 *
 * An instance of this class can be constructed by calling the no-arg constructor and then chaining method calls:
 *
 * new TypeDeclarationParameters().setKind(Type.class).setName(className) etc.
 */
public class TypeDeclarationParameters {

    private DeclaredTypeName className;
    private JavaFileWriter.Type kind;
    private List<Modifier> modifiers;
    private DeclaredTypeName superclass;
    private List<? extends TypeName> interfaces;
    
    public DeclaredTypeName getClassName() {
        return className;
    }
    
    public TypeDeclarationParameters setName(DeclaredTypeName className) {
        this.className = className;
        return this;
    }
    
    public JavaFileWriter.Type getKind() {
        return kind;
    }
    
    public TypeDeclarationParameters setKind(JavaFileWriter.Type kind) {
        this.kind = kind;
        return this;
    }
    
    public List<Modifier> getModifiers() {
        return modifiers;
    }

    public TypeDeclarationParameters setModifiers(Modifier... modifiers) {
        this.modifiers = AptUtils.asList(modifiers);
        return this;
    }
    
    public TypeDeclarationParameters setModifiers(List<Modifier> modifiers) {
        this.modifiers = modifiers;
        return this;
    }
    
    public DeclaredTypeName getSuperclass() {
        return superclass;
    }

    public TypeDeclarationParameters setSuperclass(DeclaredTypeName superclass) {
        this.superclass = superclass;
        return this;
    }

    public List<? extends TypeName> getInterfaces() {
        return interfaces;
    }

    public TypeDeclarationParameters setInterfaces(List<? extends TypeName> interfaces) {
        this.interfaces = interfaces;
        return this;
    }

    
    
}
