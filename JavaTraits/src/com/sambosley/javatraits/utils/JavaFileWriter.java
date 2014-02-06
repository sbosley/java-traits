/**
 * Copyright 2014 Yahoo Inc.
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.sambosley.javatraits.utils;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;

import com.sambosley.javatraits.utils.TypeName.TypeNameVisitor;

public class JavaFileWriter {

    private static final String INDENT = "    ";
    
    private Writer out;
    private Map<String, List<ClassName>> knownNames;
    private Scope currentScope = Scope.PACKAGE;
    
    private static enum Scope {
        PACKAGE,
        IMPORTS,
        TYPE_DECLARATION,
        TYPE_DEFINITION,
        FIELD_DECLARATION,
        METHOD_DECLARATION,
        METHOD_DEFINITION,
        FINISHED
    }
    
    public JavaFileWriter(Writer out) {
        if (out == null)
            throw new IllegalArgumentException("Writer must be non-null");
        this.out = out;
        this.knownNames = new HashMap<String, List<ClassName>>();
    }
    
    public void close() throws IOException {
        out.close();
    }
    
    public void writePackage(String packageName) throws IOException {
        checkScope(Scope.PACKAGE);
        out.append("package ").append(packageName).append(";\n\n");
        moveToScope(Scope.IMPORTS);
    }
    
    public void writeImports(Collection<ClassName> imports) throws IOException {
        checkScope(Scope.IMPORTS);
        for (ClassName item : imports) {
            String simpleName = item.getSimpleName();
            List<ClassName> allNames = knownNames.get(simpleName);
            if (allNames == null) {
                allNames = new ArrayList<ClassName>();
                knownNames.put(simpleName, allNames);
            }
            
            if (!allNames.contains(item)) {
                out.append("import ").append(item.toString()).append(";\n");
                allNames.add(item);
            }
        }
        out.append("\n");
    }

    public void beginTypeDeclaration(String name, String kind, Modifier... modifiers) throws IOException {
        beginTypeDeclaration(name, kind, Arrays.asList(modifiers));
    }
    
    public void beginTypeDeclaration(String name, String kind, List<Modifier> modifiers) throws IOException {
        checkScope(Scope.IMPORTS);
        emitModifierList(modifiers);
        out.append(kind).append(" ").append(name);
        moveToScope(Scope.TYPE_DECLARATION);
    }
    
    public void appendGenericDeclaration(List<? extends TypeName> generics) throws IOException {
        checkScope(Scope.TYPE_DECLARATION);
        emitGenericsList(generics, true);
    }
    
    public void addSuperclassToTypeDeclaration(ClassName superclass, List<? extends TypeName> generics) throws IOException {
        checkScope(Scope.TYPE_DECLARATION);
        out.append(" extends ").append(shortenName(superclass));
        emitGenericsList(generics, false);
    }
    
    public void addInterfacesToTypeDeclaration(List<ClassName> interfaces, List<List<? extends TypeName>> generics) throws IOException {
        checkScope(Scope.TYPE_DECLARATION);
        if (interfaces != null && generics != null && interfaces.size() != generics.size())
            throw new IllegalArgumentException("When specifying generics for implementing interfaces, lists must be the same size");
        out.append(" implements ");
        for (int i = 0; i < interfaces.size(); i++) {
            out.append(shortenName(interfaces.get(i)));
            List<? extends TypeName> genericsForInterface = generics.get(i);
            emitGenericsList(genericsForInterface, false);
            if (i < interfaces.size() - 1)
                out.append(", ");
        }
    }
    
    public void finishTypeDeclarationAndBeginTypeDefinition() throws IOException {
        checkScope(Scope.TYPE_DECLARATION);        
        out.append(" {\n\n");
        moveToScope(Scope.TYPE_DEFINITION);
    }

    public void emitFieldDeclaration(TypeName type, String name, List<? extends TypeName> generics, Modifier... modifiers) throws IOException {
        emitFieldDeclaration(type, name, generics, Arrays.asList(modifiers));
    }
    
    public void emitFieldDeclaration(TypeName type, String name, List<? extends TypeName> generics, List<Modifier> modifiers) throws IOException {
        checkScope(Scope.TYPE_DEFINITION);
        indent(1);
        moveToScope(Scope.FIELD_DECLARATION);
        emitModifierList(modifiers);
        out.append(shortenName(type));
        emitGenericsList(generics, false);
        out.append(" ").append(name).append(";\n");
        moveToScope(Scope.TYPE_DEFINITION);
    }
    
    public void beginMethodDeclaration(String name, TypeName returnType, List<Modifier> modifiers, List<? extends TypeName> generics) throws IOException {
        checkScope(Scope.TYPE_DEFINITION);
        indent(1);
        moveToScope(Scope.METHOD_DECLARATION);
        emitModifierList(modifiers);
        if (emitGenericsList(generics, true))
            out.append(" ");
        if (returnType == null)
            out.append("void");
        else
            out.append(shortenName(returnType));
        out.append(" ").append(name).append("("); 
    }
    
    public void beginConstructorDeclaration(String type, Modifier... modifiers) throws IOException {
        beginConstructorDeclaration(type, Arrays.asList(modifiers));
    }
    
    public void beginConstructorDeclaration(String type, List<Modifier> modifiers) throws IOException {
        checkScope(Scope.TYPE_DEFINITION);
        indent(1);
        moveToScope(Scope.METHOD_DECLARATION);
        emitModifierList(modifiers);
        out.append(type).append("(");
    }
    
    public void addArgumentList(List<? extends TypeName> argTypes, List<List<? extends TypeName>> genericsForArgs, List<String> argNames) throws IOException {
        checkScope(Scope.METHOD_DECLARATION);
        if (argTypes != null) {
            if (genericsForArgs != null && argTypes.size() != genericsForArgs.size())
                throw new IllegalArgumentException("argTypes.size() != genericsForArgs.size(). When supplying generics for arguments, lists must be the same type.");
            for (int i = 0; i < argTypes.size(); i++) {
                TypeName argType = argTypes.get(i);
                String argName = argNames.get(i);
                out.append(shortenName(argType));
                List<? extends TypeName> generics = genericsForArgs != null ? genericsForArgs.get(i) : null;
                emitGenericsList(generics, false);
                out.append(" ").append(argName);
                if (i < argTypes.size() - 1)
                    out.append(", ");
            }
        }
    }
    
    public void finishMethodDeclarationAndBeginMethodDefinition(List<? extends TypeName> thrownTypes, boolean wasAbstract) throws IOException {
        checkScope(Scope.METHOD_DECLARATION);
        out.append(")");
        if (thrownTypes != null && thrownTypes.size() > 0) {
            out.append(" throws ");
            for (int i = 0; i < thrownTypes.size(); i++) {
                out.append(shortenName(thrownTypes.get(i)));
                if (i < thrownTypes.size() - 1)
                    out.append(", ");
            }
        }
        if (wasAbstract) {
            out.append(";\n\n");
            moveToScope(Scope.TYPE_DEFINITION);
        } else {
            out.append(" {\n");
            moveToScope(Scope.METHOD_DEFINITION);
        }
    }
    
    public void emitStatement(String statement, int indentLevel) throws IOException {
        checkScope(Scope.METHOD_DEFINITION);
        indent(indentLevel);
        out.append(statement);
    }
    
    public void emitNewline() throws IOException {
        out.append("\n");
    }
    
    public void finishMethodDefinition() throws IOException {
        checkScope(Scope.METHOD_DEFINITION);
        indent(1);
        out.append("}\n\n");
        moveToScope(Scope.TYPE_DEFINITION);
    }
    
    public void finishTypeDefinitionAndCloseType() throws IOException {
        checkScope(Scope.TYPE_DEFINITION);
        out.append("}\n");
        moveToScope(Scope.FINISHED);
    }
    
    private void emitModifierList(List<Modifier> modifiers) throws IOException {
        if (modifiers != null) {
            for (Modifier mod : modifiers) {
                out.append(mod.toString()).append(" ");
            }
        }
    }
    
    private TypeNameVisitor<String, Boolean> genericDeclarationVisitor = new TypeNameVisitor<String, Boolean>() {

        @Override
        public String visitClassName(ClassName typeName, Boolean param) {
            return shortenName(typeName);
        }

        @Override
        public String visitGenericName(GenericName genericName, Boolean param) {
            StringBuilder result = new StringBuilder(genericName.getGenericName());
            if (param && genericName.getUpperBound() != null)
                result.append(" extends ").append(shortenName(genericName.getUpperBound()));
            return result.toString();
        }
    };
    
    public boolean emitGenericsList(List<? extends TypeName> generics, boolean includeBounds) throws IOException {
        if (generics == null || generics.size() == 0)
            return false;
        out.append("<");
        
        for (int i = 0; i < generics.size(); i++) {
            TypeName generic = generics.get(i);
            out.append(generic.accept(genericDeclarationVisitor, includeBounds));
            if (i < generics.size() - 1)
                out.append(", ");
        }
        out.append(">");
        return true;
    }
    
    private void indent(int times) throws IOException {
        for (int i = 0; i < times; i++)
            out.append(INDENT);
    }
    
    private TypeNameVisitor<String, Map<String, List<ClassName>>> nameShorteningVisitor = new TypeNameVisitor<String, Map<String,List<ClassName>>>() {
        
        @Override
        public String visitGenericName(GenericName genericName, Map<String, List<ClassName>> param) {
            return genericName.getTypeString(true);
        }
        
        @Override
        public String visitClassName(ClassName typeName, Map<String, List<ClassName>> param) {
            String simpleName = typeName.getSimpleName();
            List<ClassName> allNames = param.get(simpleName);
            boolean simple;
            if (allNames == null || allNames.size() == 0)
                simple = false;
            else if (allNames.get(0).equals(typeName))
                simple = true;
            else
                simple = false;
            return typeName.getTypeString(simple);
        }
    };
    
    public String shortenName(TypeName name) {
        return name.accept(nameShorteningVisitor, knownNames);
    }
    
    private void checkScope(Scope expectedScope) {
        if (currentScope != expectedScope)
            throw new IllegalStateException("Expected scope " + expectedScope + ", current scope " + currentScope);
    }
    
    private void moveToScope(Scope moveTo) {
        this.currentScope = moveTo;
    }
}
