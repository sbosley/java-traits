/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.annotations.writer;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.lang.model.element.Modifier;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.model.GenericName;
import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.model.TypeName.TypeNameVisitor;
import com.yahoo.annotations.utils.Utils;
import com.yahoo.annotations.writer.parameters.MethodDeclarationParameters;
import com.yahoo.annotations.writer.parameters.TypeDeclarationParameters;

public class JavaFileWriter {

    private static final String INDENT = "    ";

    private Writer out;
    private Map<String, List<DeclaredTypeName>> knownNames;
    private Type kind = null;
    private Deque<Scope> scopeStack = new LinkedList<Scope>();

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
        METHOD_DEFINITION,
    }

    public JavaFileWriter(Writer out) {
        if (out == null) {
            throw new IllegalArgumentException("Writer must be non-null");
        }
        this.out = out;
        this.knownNames = new HashMap<String, List<DeclaredTypeName>>();
        scopeStack.push(Scope.PACKAGE);
    }

    public void close() throws IOException {
        out.close();
    }

    public void writePackage(String packageName) throws IOException {
        checkScope(Scope.PACKAGE);
        out.append("package ").append(packageName).append(";\n\n");
        finishScope(Scope.PACKAGE);
        moveToScope(Scope.IMPORTS);
    }

    public void writeImports(Collection<DeclaredTypeName> imports) throws IOException {
        checkScope(Scope.IMPORTS);
        TreeSet<String> sortedImports = new TreeSet<String>();
        for (DeclaredTypeName item : imports) {
            String simpleName = item.getSimpleName();
            List<DeclaredTypeName> allNames = knownNames.get(simpleName);
            if (allNames == null) {
                allNames = new ArrayList<DeclaredTypeName>();
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
        for (String item : sortedImports) {
            out.append("import ").append(item).append(";\n");
        }
        out.append("\n");
        finishScope(Scope.IMPORTS);
    }

    public void beginTypeDefinition(TypeDeclarationParameters typeDeclaration) throws IOException {
        validateTypeDeclarationParams(typeDeclaration);
        indent();

        boolean isRootClass = Utils.isEmpty(scopeStack);
        if (!isRootClass) {
            checkScope(Scope.TYPE_DEFINITION); // Begin a new inner type definition 
        }
        
        this.kind = typeDeclaration.getKind();
        writeModifierList(typeDeclaration.getModifiers());
        out.append(typeDeclaration.getKind().name).append(" ").append(typeDeclaration.getClassName().getSimpleName());
        writeGenericsList(typeDeclaration.getClassName().getTypeArgs(), true);

        if (typeDeclaration.getSuperclass() != null && !Utils.OBJECT_CLASS_NAME.equals(typeDeclaration.getSuperclass().toString())) {
            out.append(" extends ").append(shortenName(typeDeclaration.getSuperclass(), false));
        }

        if (!Utils.isEmpty(typeDeclaration.getInterfaces())) {
            out.append(" implements ");
            for (int i = 0; i < typeDeclaration.getInterfaces().size(); i++) {
                out.append(shortenName(typeDeclaration.getInterfaces().get(i), false));
                if (i < typeDeclaration.getInterfaces().size() - 1) {
                    out.append(", ");
                }
            }
        }
        out.append(" {\n\n");
        moveToScope(Scope.TYPE_DEFINITION);
    }

    private void validateTypeDeclarationParams(TypeDeclarationParameters params) {
        if (params.getClassName() == null) {
            throw new IllegalArgumentException("Must specify a class name for TypeDeclarationParameters");
        }
        if (params.getKind() == null) {
            throw new IllegalArgumentException("Must specify a type for TypeDeclarationParameters (one of Type.CLASS or Type.INTERFACE)");
        }
    }

    public static interface FieldInitializationExpression {
        public void writeInitializationString(JavaFileWriter writer) throws IOException;
    }

    public static class ConstructorInitialization implements FieldInitializationExpression {
        private final DeclaredTypeName constructorType;
        private final List<String> argumentNames;

        public ConstructorInitialization(DeclaredTypeName constructorType, List<String> argumentNames) {
            this.constructorType = constructorType;
            this.argumentNames = argumentNames;
        }
        
        public void writeInitializationString(JavaFileWriter writer) throws IOException {
            writer.out.append("new ").append(writer.shortenName(constructorType, false));
            writer.writeTypelessArgumentList(argumentNames);
        }
    }
    
    public static class AnonymousInnerClassInitialization extends ConstructorInitialization {
        
        public AnonymousInnerClassInitialization(DeclaredTypeName constructorType, List<String> argumentNames) {
            super(constructorType, argumentNames);
        }

        @Override
        public void writeInitializationString(JavaFileWriter writer) throws IOException {
            super.writeInitializationString(writer);
            writer.out.append(" {\n");
            writer.moveToScope(Scope.TYPE_DEFINITION);
        }
    }

    public void writeFieldDeclaration(TypeName type, String name, List<Modifier> modifiers, FieldInitializationExpression initializer) throws IOException {
        checkScope(Scope.TYPE_DEFINITION);
        indent();
        writeModifierList(modifiers);
        out.append(shortenName(type, false));
        out.append(" ").append(name);
        if (initializer != null) {
            out.append(" = ");
            initializer.writeInitializationString(this);
        }
        out.append(";\n");
    }

    public void beginMethodDefinition(MethodDeclarationParameters methodDeclaration) throws IOException {
        validateMethodDefinitionParams(methodDeclaration);
        checkScope(Scope.TYPE_DEFINITION);
        indent();
        boolean isAbstract = kind.equals(Type.INTERFACE) ||
                (Utils.isEmpty(methodDeclaration.getModifiers()) ?
                        false : methodDeclaration.getModifiers().contains(Modifier.ABSTRACT));
        writeModifierList(methodDeclaration.getModifiers());
        if (writeGenericsList(methodDeclaration.getMethodGenerics(), true)) {
            out.append(" ");
        }
        if (methodDeclaration.getReturnType() == null) {
            out.append("void");
        } else {
            out.append(shortenName(methodDeclaration.getReturnType(), false));
        }
        out.append(" ").append(methodDeclaration.getMethodName());
        writeArgumentList(methodDeclaration.getArgumentTypes(), methodDeclaration.getArgumentNames());
        if (!Utils.isEmpty(methodDeclaration.getThrowsTypes())) {
            out.append(" throws ");
            for (int i = 0; i < methodDeclaration.getThrowsTypes().size(); i++) {
                out.append(shortenName(methodDeclaration.getThrowsTypes().get(i), false));
                if (i < methodDeclaration.getThrowsTypes().size() - 1) {
                    out.append(", ");
                }
            }
        }
        if (isAbstract) {
            out.append(";\n\n");
        } else {
            out.append(" {\n");
            moveToScope(Scope.METHOD_DEFINITION);
        }
    }

    private void validateMethodDefinitionParams(MethodDeclarationParameters params) {
        if (Utils.isEmpty(params.getMethodName())) {
            throw new IllegalArgumentException("Must specify a method name for MethodDeclarationParams");
        }
        verifyArgumentTypesAndNames(params.getArgumentTypes(), params.getArgumentNames());
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
        if (argumentNames != null) {
            for (int i = 0; i < argumentNames.size(); i++) {
                TypeName argType = argumentTypes != null ? argumentTypes.get(i) : null;
                String argName = argumentNames.get(i);
                if (argType != null) {
                    out.append(shortenName(argType, false)).append(" ");
                }
                out.append(argName);
                if (i < argumentNames.size() - 1) {
                    out.append(", ");
                }
            }
        }
        out.append(")");
    }

    private void writeTypelessArgumentList(List<String> argumentNames) throws IOException {
        writeArgumentList(null, argumentNames);
    }

    public void beginConstructorDeclaration(MethodDeclarationParameters constructorDeclaration) throws IOException {
        verifyConstructorDeclarationParams(constructorDeclaration);
        checkScope(Scope.TYPE_DEFINITION);
        indent();
        writeModifierList(constructorDeclaration.getModifiers());
        out.append(constructorDeclaration.getConstructorName().getSimpleName());
        writeGenericsList(constructorDeclaration.getMethodGenerics(), false);
        writeArgumentList(constructorDeclaration.getArgumentTypes(), constructorDeclaration.getArgumentNames());
        out.append(" {\n");
        moveToScope(Scope.METHOD_DEFINITION);
    }

    private void verifyConstructorDeclarationParams(MethodDeclarationParameters params) {
        if (!params.isConstructor()) {
            throw new IllegalArgumentException("Must specify a class name for ConstructorDeclarationParams");
        }
        verifyArgumentTypesAndNames(params.getArgumentTypes(), params.getArgumentNames());
    }

    public void writeMethodBodyStatement(String statement) throws IOException {
        checkScope(Scope.METHOD_DEFINITION);
        indent();
        out.append(statement);
    }

    public void writeNewline() throws IOException {
        out.append("\n");
    }

    public void finishMethodDefinition() throws IOException {
        finishScope(Scope.METHOD_DEFINITION);
        indent();
        out.append("}\n\n");
    }

    public void finishTypeDefinition() throws IOException {
        finishScope(Scope.TYPE_DEFINITION);
        out.append("}\n");
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
        if (!genericsList.isEmpty()) {
            out.append(genericsList);
        }
        return !genericsList.isEmpty();
    }

    public String getGenericsListString(List<? extends TypeName> generics, boolean includeBounds) {
        if (Utils.isEmpty(generics)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("<");

        for (int i = 0; i < generics.size(); i++) {
            TypeName generic = generics.get(i);
            builder.append(shortenName(generic, includeBounds));
            if (i < generics.size() - 1) {
                builder.append(", ");
            }
        }
        builder.append(">");
        return builder.toString();
    }

    private void indent() throws IOException {
        int indentLevel = scopeStack.size();
        for (int i = 0; i < indentLevel; i++) {
            out.append(INDENT);
        }
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
                        boolean recursiveUpperBounds = includeGenericBounds && bound instanceof DeclaredTypeName;
                        builder.append(shortenName(bound, recursiveUpperBounds));
                        builder.append(separator);
                    }
                    builder.delete(builder.length() - separator.length(), builder.length());
                }
                if (genericName.hasSuperBound()) {
                    boolean recursiveUpperBounds = includeGenericBounds && genericName.getSuperBound() instanceof DeclaredTypeName;
                    builder.append(" super ").append(shortenName(genericName.getSuperBound(), recursiveUpperBounds));
                }
            }
            return builder.append(genericName.getArrayStringSuffix()).toString();
        }

        @Override
        public String visitClassName(DeclaredTypeName typeName, Boolean includeGenericBounds) {
            String simpleName = typeName.getSimpleName();
            List<DeclaredTypeName> allNames = knownNames.get(simpleName);
            boolean simple;
            if (allNames == null || allNames.size() == 0) {
                simple = false;
            } else if (allNames.get(0).equals(typeName)) {
                simple = true;
            } else {
                simple = false;
            }
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

    private Scope getCurrentScope() {
        return scopeStack.peek();
    }

    private void checkScope(Scope expectedScope) {
        Scope currentScope = getCurrentScope();
        if (currentScope != expectedScope) {
            throw new IllegalStateException("Expected scope " + expectedScope + ", current scope " + currentScope);
        }
    }

    private void moveToScope(Scope moveTo) {
        scopeStack.push(moveTo);
    }

    private void finishScope(Scope expectedFinishScope) {
        checkScope(expectedFinishScope);
        scopeStack.pop();
    }
}
