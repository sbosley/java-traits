/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.annotations;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.lang.model.element.Modifier;

import com.yahoo.annotations.TypeName.TypeNameVisitor;

public class JavaFileWriter {

    private static final String INDENT = "    ";

    private Writer out;
    private Map<String, List<ClassName>> knownNames;
    private Type kind = null;
    private Scope currentScope = Scope.PACKAGE;

    public static enum Type {
        CLASS("class"),
        INTERFACE("interface");


        private String name;
        private Type(String name) {
            this.name = name;
        }
    }

    private static enum Scope {
        PACKAGE,
        IMPORTS,
        TYPE_DEFINITION,
        FIELD_DECLARATION,
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
        TreeSet<String> sortedImports = new TreeSet<String>();
        for (ClassName item : imports) {
            String simpleName = item.getSimpleName();
            List<ClassName> allNames = knownNames.get(simpleName);
            if (allNames == null) {
                allNames = new ArrayList<ClassName>();
                knownNames.put(simpleName, allNames);
            }

            if (!allNames.contains(item)) {
                if (item.isJavaLangPackage()) {
                    allNames.add(0, item);
                } else {
                    allNames.add(item);
                    sortedImports.add(item.toString());
                }
            }
        }
        for (String item : sortedImports)
            out.append("import ").append(item).append(";\n");
        out.append("\n");
    }

    public static class TypeDeclarationParameters {
        public ClassName name;
        public Type kind;
        public List<Modifier> modifiers;
        public ClassName superclass;
        public List<ClassName> interfaces;
    }

    public void beginTypeDefinition(TypeDeclarationParameters typeDeclaration) throws IOException {
        validateTypeDeclarationParams(typeDeclaration);
        checkScope(Scope.IMPORTS);
        this.kind = typeDeclaration.kind;
        writeModifierList(typeDeclaration.modifiers);
        out.append(typeDeclaration.kind.name).append(" ").append(typeDeclaration.name.getSimpleName());
        writeGenericsList(typeDeclaration.name.getTypeArgs(), true);

        if (typeDeclaration.superclass != null && !Utils.OBJECT_CLASS_NAME.equals(typeDeclaration.superclass.toString())) {
            out.append(" extends ").append(shortenName(typeDeclaration.superclass, false));
        }

        if (!Utils.isEmpty(typeDeclaration.interfaces)) {
            out.append(" implements ");
            for (int i = 0; i < typeDeclaration.interfaces.size(); i++) {
                out.append(shortenName(typeDeclaration.interfaces.get(i), false));
                if (i < typeDeclaration.interfaces.size() - 1)
                    out.append(", ");
            }
        }
        out.append(" {\n\n");
        moveToScope(Scope.TYPE_DEFINITION);
    }

    private void validateTypeDeclarationParams(TypeDeclarationParameters params) {
        if (params.name == null) {
            throw new IllegalArgumentException("Must specify a class name for TypeDeclarationParameters");
        }
        if (params.kind == null) {
            throw new IllegalArgumentException("Must specify a type for TypeDeclarationParameters (one of Type.CLASS or Type.INTERFACE)");
        }
    }

    public void writeFieldDeclaration(TypeName type, String name, List<Modifier> modifiers) throws IOException {
        checkScope(Scope.TYPE_DEFINITION);
        indent(1);
        moveToScope(Scope.FIELD_DECLARATION);
        writeModifierList(modifiers);
        out.append(shortenName(type, false));
        out.append(" ").append(name).append(";\n");
        moveToScope(Scope.TYPE_DEFINITION);
    }

    public static class MethodDeclarationParams {
        public String name;
        public TypeName returnType;
        public List<Modifier> modifiers;
        public List<? extends TypeName> methodGenerics;
        public List<? extends TypeName> argumentTypes;
        public List<String> argumentNames;
        public List<? extends TypeName> throwsTypes;
    }

    public void beginMethodDefinition(MethodDeclarationParams methodDeclaration) throws IOException {
        validateMethodDefinitionParams(methodDeclaration);
        checkScope(Scope.TYPE_DEFINITION);
        indent(1);
        boolean isAbstract = kind.equals(Type.INTERFACE) ||
                (Utils.isEmpty(methodDeclaration.modifiers) ?
                        false : methodDeclaration.modifiers.contains(Modifier.ABSTRACT));
        writeModifierList(methodDeclaration.modifiers);
        if (writeGenericsList(methodDeclaration.methodGenerics, true))
            out.append(" ");
        if (methodDeclaration.returnType == null)
            out.append("void");
        else {
            out.append(shortenName(methodDeclaration.returnType, false));
        }
        out.append(" ").append(methodDeclaration.name);
        writeArgumentList(methodDeclaration.argumentTypes, methodDeclaration.argumentNames);
        if (!Utils.isEmpty(methodDeclaration.throwsTypes)) {
            out.append(" throws ");
            for (int i = 0; i < methodDeclaration.throwsTypes.size(); i++) {
                out.append(shortenName(methodDeclaration.throwsTypes.get(i), false));
                if (i < methodDeclaration.throwsTypes.size() - 1)
                    out.append(", ");
            }
        }
        if (isAbstract) {
            out.append(";\n\n");
        } else {
            out.append(" {\n");
            moveToScope(Scope.METHOD_DEFINITION);
        }
    }

    private void validateMethodDefinitionParams(MethodDeclarationParams params) {
        if (Utils.isEmpty(params.name)) {
            throw new IllegalArgumentException("Must specify a method name for MethodDeclarationParams");
        }
        verifyArgumentTypesAndNames(params.argumentTypes, params.argumentNames);
    }

    private void verifyArgumentTypesAndNames(List<? extends TypeName> argumentTypes, List<String> argumentNames) {
        if (Utils.isEmpty(argumentTypes) && !Utils.isEmpty(argumentNames)) {
            throw new IllegalArgumentException("Must specify argument types for MethodDeclarationParams");
        }
        if (!Utils.isEmpty(argumentTypes) && Utils.isEmpty(argumentNames)) {
            throw new IllegalArgumentException("Must specify argument names for MethodDeclarationParams");
        }
        if (!Utils.isEmpty(argumentTypes) && !Utils.isEmpty(argumentNames)
                && argumentTypes.size() != argumentNames.size()) {
            String error = "Different number of argument types and names in MethodDeclarationParams. "
                    + argumentTypes.size() + " types, " + argumentNames.size() + " names.";
            throw new IllegalArgumentException(error);
        }
    }

    private void writeArgumentList(List<? extends TypeName> argumentTypes, List<String> argumentNames) throws IOException {
        out.append("(");
        if (argumentTypes != null) {
            for (int i = 0; i < argumentTypes.size(); i++) {
                TypeName argType = argumentTypes.get(i);
                String argName = argumentNames.get(i);
                out.append(shortenName(argType, false));
                out.append(" ").append(argName);
                if (i < argumentTypes.size() - 1)
                    out.append(", ");
            }
        }
        out.append(")");
    }

    public static class ConstructorDeclarationParams {
        public ClassName name;
        public List<Modifier> modifiers;
        public List<? extends TypeName> methodGenerics;
        public List<? extends TypeName> argumentTypes;
        public List<String> argumentNames;
    }

    public void beginConstructorDeclaration(ConstructorDeclarationParams constructorDeclaration) throws IOException {
        verifyConstructorDeclarationParams(constructorDeclaration);
        checkScope(Scope.TYPE_DEFINITION);
        indent(1);
        writeModifierList(constructorDeclaration.modifiers);
        out.append(constructorDeclaration.name.getSimpleName());
        writeGenericsList(constructorDeclaration.methodGenerics, false);
        writeArgumentList(constructorDeclaration.argumentTypes, constructorDeclaration.argumentNames);
        out.append(" {\n");
        moveToScope(Scope.METHOD_DEFINITION);
    }

    private void verifyConstructorDeclarationParams(ConstructorDeclarationParams params) {
        if (params.name == null) {
            throw new IllegalArgumentException("Must specify a method name for ConstructorDeclarationParams");
        }
        verifyArgumentTypesAndNames(params.argumentTypes, params.argumentNames);
    }

    public void writeStatement(String statement, int indentLevel) throws IOException {
        checkScope(Scope.METHOD_DEFINITION);
        indent(indentLevel);
        out.append(statement);
    }

    public void writeNewline() throws IOException {
        out.append("\n");
    }

    public void finishMethodDefinition() throws IOException {
        checkScope(Scope.METHOD_DEFINITION);
        indent(1);
        out.append("}\n\n");
        moveToScope(Scope.TYPE_DEFINITION);
    }

    public void finishTypeDefinition() throws IOException {
        checkScope(Scope.TYPE_DEFINITION);
        out.append("}\n");
        moveToScope(Scope.FINISHED);
    }

    private void writeModifierList(List<Modifier> modifiers) throws IOException {
        if (modifiers != null) {
            for (Modifier mod : modifiers) {
                out.append(mod.toString()).append(" ");
            }
        }
    }

    private boolean writeGenericsList(List<? extends TypeName> generics, boolean includeBounds) throws IOException {
        String genericsList = getGenericsListString(generics, includeBounds);
        if (!genericsList.isEmpty())
            out.append(genericsList);
        return !genericsList.isEmpty();
    }

    public String getGenericsListString(List<? extends TypeName> generics, boolean includeBounds) {
        if (Utils.isEmpty(generics))
            return "";
        StringBuilder builder = new StringBuilder();
        builder.append("<");

        for (int i = 0; i < generics.size(); i++) {
            TypeName generic = generics.get(i);
            builder.append(shortenName(generic, includeBounds));
            if (i < generics.size() - 1)
                builder.append(", ");
        }
        builder.append(">");
        return builder.toString();
    }

    private void indent(int times) throws IOException {
        for (int i = 0; i < times; i++)
            out.append(INDENT);
    }

    private TypeNameVisitor<String, Boolean> nameShorteningVisitor = new TypeNameVisitor<String, Boolean>() {

        @Override
        public String visitGenericName(GenericName genericName, Boolean includeGenericBounds) {
            StringBuilder builder = new StringBuilder(genericName.getGenericName());
            if (genericName.isWildcard() || includeGenericBounds) {
                if (genericName.hasExtendsBound()) {
                    builder.append(" extends ");
                    String separator = " & ";
                    for (TypeName bound : genericName.getExtendsBound()) {
                        boolean recursiveUpperBounds = includeGenericBounds && bound instanceof ClassName;
                        builder.append(shortenName(bound, recursiveUpperBounds));
                        builder.append(separator);
                    }
                    builder.delete(builder.length() - separator.length(), builder.length());
                }
                if (genericName.hasSuperBound()) {
                    boolean recursiveUpperBounds = includeGenericBounds && genericName.getSuperBound() instanceof ClassName;
                    builder.append(" super ").append(shortenName(genericName.getSuperBound(), recursiveUpperBounds));
                }
            }
            return builder.append(genericName.getArrayStringSuffix()).toString();
        }

        @Override
        public String visitClassName(ClassName typeName, Boolean includeGenericBounds) {
            String simpleName = typeName.getSimpleName();
            List<ClassName> allNames = knownNames.get(simpleName);
            boolean simple;
            if (allNames == null || allNames.size() == 0)
                simple = false;
            else if (allNames.get(0).equals(typeName))
                simple = true;
            else
                simple = false;
            StringBuilder nameBuilder = new StringBuilder();
            String nameBase = simple ? typeName.getSimpleName() : typeName.toString();
            nameBuilder.append(nameBase);
            nameBuilder.append(getGenericsListString(typeName.getTypeArgs(), includeGenericBounds));
            nameBuilder.append(typeName.getArrayStringSuffix());
            return nameBuilder.toString();
        }
    };

    public String shortenName(TypeName name, boolean includeGenericBounds) {
        return name.accept(nameShorteningVisitor, includeGenericBounds);
    }

    private void checkScope(Scope expectedScope) {
        if (currentScope != expectedScope)
            throw new IllegalStateException("Expected scope " + expectedScope + ", current scope " + currentScope);
    }

    private void moveToScope(Scope moveTo) {
        this.currentScope = moveTo;
    }
}
