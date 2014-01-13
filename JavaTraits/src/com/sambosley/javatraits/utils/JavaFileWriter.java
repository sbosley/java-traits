package com.sambosley.javatraits.utils;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;

public class JavaFileWriter {

    private static final String INDENT = "    ";
    
    private Writer out;
    private Map<String, List<FullyQualifiedName>> knownNames;
    private Scope currentScope = Scope.IMPORTS;
    
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
        this.knownNames = new HashMap<String, List<FullyQualifiedName>>();
    }
    
    public void close() throws IOException {
        out.close();
    }
    
    public void writePackage(String packageName) throws IOException {
        checkScope(Scope.PACKAGE);
        out.append("package ").append(packageName).append(";\n\n");
    }
    
    public void writeImports(Collection<FullyQualifiedName> imports) throws IOException {
        checkScope(Scope.IMPORTS);
        for (FullyQualifiedName item : imports) {
            String simpleName = item.getSimpleName();
            List<FullyQualifiedName> allNames = knownNames.get(simpleName);
            if (allNames == null) {
                allNames = new ArrayList<FullyQualifiedName>();
                knownNames.put(simpleName, allNames);
            }
            
            if (!allNames.contains(item)) {
                out.append("import ").append(item.toString()).append(";\n");
                allNames.add(item);
            }
        }
        out.append("\n");
    }
    
    public void beginTypeDeclaration(String name, String kind, List<Modifier> modifiers) throws IOException {
        checkScope(Scope.IMPORTS);
        emitModifierList(modifiers);
        out.append(kind).append(" ").append(name);
        moveToScope(Scope.TYPE_DECLARATION);
    }
    
    public void appendGenericDeclaration(List<String> generics, List<FullyQualifiedName> bounds) throws IOException {
        checkScope(Scope.TYPE_DECLARATION);
        emitGenericsList(generics, bounds);
    }
    
    public void addSuperclassToTypeDeclaration(FullyQualifiedName superclass, List<String> generics) throws IOException {
        checkScope(Scope.TYPE_DECLARATION);
        out.append(" extends ").append(shortenName(superclass));
        emitGenericsList(generics, null);
    }
    
    public void addInterfacesToTypeDeclaration(List<FullyQualifiedName> interfaces, List<List<String>> generics) throws IOException {
        checkScope(Scope.TYPE_DECLARATION);
        if (interfaces != null && generics != null && interfaces.size() != generics.size())
            throw new IllegalArgumentException("When specifying generics for implementing interfaces, lists must be the same size");
        out.append(" implements ");
        for (int i = 0; i < interfaces.size(); i++) {
            out.append(shortenName(interfaces.get(i)));
            List<String> genericsForInterface = generics.get(i);
            emitGenericsList(genericsForInterface, null);
            if (i < interfaces.size() - 1)
                out.append(", ");
        }
    }
    
    public void finishTypeDeclarationAndBeginTypeDefinition() throws IOException {
        checkScope(Scope.TYPE_DECLARATION);        
        out.append(" {\n\n");
        moveToScope(Scope.TYPE_DEFINITION);
    }
    
    public void emitFieldDeclaration(FullyQualifiedName type, String name, List<String> generics, List<Modifier> modifiers) throws IOException {
        checkScope(Scope.TYPE_DEFINITION);
        indent(1);
        moveToScope(Scope.FIELD_DECLARATION);
        emitModifierList(modifiers);
        out.append(shortenName(type));
        emitGenericsList(generics, null);
        out.append(" ").append(name).append(";\n");
        moveToScope(Scope.TYPE_DEFINITION);
    }
    
    public void beginMethodDeclaration(String name, FullyQualifiedName returnType, List<Modifier> modifiers, List<String> generics, List<FullyQualifiedName> bounds) throws IOException {
        checkScope(Scope.TYPE_DEFINITION);
        indent(1);
        moveToScope(Scope.METHOD_DECLARATION);
        emitModifierList(modifiers);
        if (emitGenericsList(generics, bounds))
            out.append(" ");
        out.append(shortenName(returnType)) // TODO: Handle array types, generic return types, primitive return types
            .append(" ").append(name).append("("); 
    }
    
    public void addArgumentList(List<FullyQualifiedName> argTypes, List<String> argNames) throws IOException { // TODO: handle array types, generic types, primitive types
        checkScope(Scope.METHOD_DECLARATION);
        // TODO: Check for validity of arguments (non-null, length, etc.)
        for (int i = 0; i < argTypes.size(); i++) {
            FullyQualifiedName argType = argTypes.get(i);
            String argName = argNames.get(i);
            out.append(shortenName(argType)).append(" ").append(argName);
            if (i < argTypes.size() - 1)
                out.append(", ");
        }
    }
    
    public void finishMethodDeclarationAndBeginMethodDefinition(boolean wasAbstract) throws IOException {
        checkScope(Scope.METHOD_DECLARATION);
        out.append(")");
        if (wasAbstract) {
            out.append(";\n\n");
            moveToScope(Scope.TYPE_DEFINITION);
        } else {
            out.append(" {\n");
            moveToScope(Scope.METHOD_DEFINITION);
        }
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
    
    private boolean emitGenericsList(List<String> generics, List<FullyQualifiedName> bounds) throws IOException {
        if (generics == null || generics.size() == 0)
            return false;
        out.append("<");
        if (generics.size() > 0 && bounds != null && generics.size() != bounds.size())
            throw new IllegalArgumentException("Generics and bounds must have the same size");
        
        for (int i = 0; i < generics.size(); i++) {
            String generic = generics.get(i);
            FullyQualifiedName bound = bounds != null ? bounds.get(i) : null;
            
            out.append(generic);
            if (bound != null)
                out.append(" extends ").append(shortenName(bound));
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
    
    private String shortenName(FullyQualifiedName name) {
        String simpleName = name.getSimpleName();
        List<FullyQualifiedName> allNames = knownNames.get(simpleName);
        if (allNames == null || allNames.size() == 0)
            return name.toString();
        if (allNames.get(0).equals(name))
            return simpleName;
        return name.toString();
    }
    
    private void checkScope(Scope expectedScope) {
        if (currentScope != expectedScope)
            throw new IllegalStateException("Expected scope " + expectedScope + ", current scope " + currentScope);
    }
    
    private void moveToScope(Scope moveTo) {
        this.currentScope = moveTo;
    }
}
