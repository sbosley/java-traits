package com.yahoo.annotations.writer.parameters;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.utils.AptUtils;
import com.yahoo.annotations.writer.JavaFileWriter.Type;

import javax.lang.model.element.Modifier;
import java.util.List;

public class TypeDeclarationParameters {

    private DeclaredTypeName className;
    private Type kind;
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
    
    public Type getKind() {
        return kind;
    }
    
    public TypeDeclarationParameters setKind(Type kind) {
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
