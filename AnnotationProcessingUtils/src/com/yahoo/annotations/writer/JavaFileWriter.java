/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.annotations.writer;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.yahoo.annotations.writer.expressions.Expression;
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

    public static enum Scope {
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

    public JavaFileWriter close() throws IOException {
        out.close();
        return this;
    }

    public JavaFileWriter writePackage(String packageName) throws IOException {
        checkScope(Scope.PACKAGE);
        out.append("package ").append(packageName).append(";\n\n");
        finishScope(Scope.PACKAGE);
        moveToScope(Scope.IMPORTS);
        return this;
    }

    public JavaFileWriter writeImports(Collection<DeclaredTypeName> imports) throws IOException {
        checkScope(Scope.IMPORTS);
        TreeSet<String> sortedImports = new TreeSet<String>();
        if (!Utils.isEmpty(imports)) {
            for (DeclaredTypeName item : imports) {
                if (addToKnownNames(item, item.isJavaLangPackage())) {
                    sortedImports.add(item.toString());
                }
            }
        }
        for (String item : sortedImports) {
            out.append("import ").append(item).append(";\n");
        }
        out.append("\n");
        finishScope(Scope.IMPORTS);
        return this;
    }
    
    // For names that can be shortened but don't need to be imported
    public JavaFileWriter registerOtherKnownNames(Collection<DeclaredTypeName> otherKnownNames) throws IOException {
        if (!Utils.isEmpty(otherKnownNames)) {
            for (DeclaredTypeName item : otherKnownNames) {
                addToKnownNames(item, false);
            }
        }
        return this;
    }
    
    // Returns true if item needs to be added to imports
    private boolean addToKnownNames(DeclaredTypeName type, boolean highestPreference) {
        String simpleName = type.getSimpleName();
        List<DeclaredTypeName> allNames = knownNames.get(simpleName);
        if (allNames == null) {
            allNames = new ArrayList<DeclaredTypeName>();
            knownNames.put(simpleName, allNames);
        }

        if (!allNames.contains(type)) {
            if (highestPreference) {
                allNames.add(0, type);
                return false;
            } else {
                allNames.add(type);
                return true;
            }
        }
        return false;
    }

    public JavaFileWriter beginTypeDefinition(TypeDeclarationParameters typeDeclaration) throws IOException {
        validateTypeDeclarationParams(typeDeclaration);
        indent();

        boolean isRootClass = Utils.isEmpty(scopeStack);
        if (!isRootClass) {
            checkScope(Scope.TYPE_DEFINITION); // Begin a new inner type definition 
        } else {
            addToKnownNames(typeDeclaration.getClassName(), true);
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
        return this;
    }

    private void validateTypeDeclarationParams(TypeDeclarationParameters params) {
        if (params.getClassName() == null) {
            throw new IllegalArgumentException("Must specify a class name for TypeDeclarationParameters");
        }
        if (params.getKind() == null) {
            throw new IllegalArgumentException("Must specify a type for TypeDeclarationParameters (one of Type.CLASS or Type.INTERFACE)");
        }
    }
    
    public JavaFileWriter writeFieldDeclaration(TypeName type, String name, Expression initializer, Modifier... modifiers) throws IOException {
        return writeFieldDeclaration(type, name, initializer, modifiers == null ? null : Arrays.asList(modifiers));
    }

    public JavaFileWriter writeFieldDeclaration(TypeName type, String name, Expression initializer, List<Modifier> modifiers) throws IOException {
        checkScope(Scope.TYPE_DEFINITION, Scope.METHOD_DEFINITION);
        indent();
        writeModifierList(modifiers);
        out.append(shortenName(type, false));
        out.append(" ").append(name);
        if (initializer != null) {
            out.append(" = ");
            appendExpression(initializer);
        }
        out.append(";\n");
        return this;
    }

    public JavaFileWriter beginMethodDefinition(MethodDeclarationParameters methodDeclaration) throws IOException {
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
        writeArgumentList(methodDeclaration.getArgumentTypes(), methodDeclaration.getArguments());
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
        return this;
    }
    
    public JavaFileWriter beginInitializerBlock(boolean isStatic, boolean indentStart) throws IOException {
        checkScope(Scope.TYPE_DEFINITION);
        if (indentStart) {
            indent();
        }
        if (isStatic) {
            out.append("static ");
        }
        out.append("{\n");
        moveToScope(Scope.METHOD_DEFINITION);
        return this;
    }

    private void validateMethodDefinitionParams(MethodDeclarationParameters params) {
        if (Utils.isEmpty(params.getMethodName())) {
            throw new IllegalArgumentException("Must specify a method name for MethodDeclarationParams");
        }
        verifyArgumentTypesAndNames(params.getArgumentTypes(), params.getArguments());
    }

    private void verifyArgumentTypesAndNames(List<? extends TypeName> argumentTypes, List<?> arguments) {
        if (Utils.isEmpty(argumentTypes) && !Utils.isEmpty(arguments)) {
            throw new IllegalArgumentException("Must specify argument types for MethodDeclarationParams");
        }
        if (!Utils.isEmpty(argumentTypes) && Utils.isEmpty(arguments)) {
            throw new IllegalArgumentException("Must specify argument names for MethodDeclarationParams");
        }
        if (!Utils.isEmpty(argumentTypes) && !Utils.isEmpty(arguments)
                && argumentTypes.size() != arguments.size()) {
            String error = "Different number of argument types and names in MethodDeclarationParams. "
                    + argumentTypes.size() + " types, " + arguments.size() + " names.";
            throw new IllegalArgumentException(error);
        }
    }

    public JavaFileWriter writeArgumentList(List<? extends TypeName> argumentTypes, List<?> arguments) throws IOException {
        out.append("(");
        if (arguments != null) {
            for (int i = 0; i < arguments.size(); i++) {
                TypeName argType = argumentTypes != null ? argumentTypes.get(i) : null;
                
                if (argType != null) {
                    out.append(shortenName(argType, false)).append(" ");
                }
                
                Object argument = arguments.get(i);
                if (argument instanceof Expression) {
                    ((Expression) argument).writeExpression(this);
                } else {
                    out.append(String.valueOf(argument));
                }
                if (i < arguments.size() - 1) {
                    out.append(", ");
                }
            }
        }
        out.append(")");
        return this;
    }

    public JavaFileWriter writeArgumentNameList(List<?> arguments) throws IOException {
        return writeArgumentList(null, arguments);
    }

    public JavaFileWriter beginConstructorDeclaration(MethodDeclarationParameters constructorDeclaration) throws IOException {
        verifyConstructorDeclarationParams(constructorDeclaration);
        checkScope(Scope.TYPE_DEFINITION);
        indent();
        writeModifierList(constructorDeclaration.getModifiers());
        out.append(constructorDeclaration.getConstructorName().getSimpleName());
        writeGenericsList(constructorDeclaration.getMethodGenerics(), false);
        writeArgumentList(constructorDeclaration.getArgumentTypes(), constructorDeclaration.getArguments());
        out.append(" {\n");
        moveToScope(Scope.METHOD_DEFINITION);
        return this;
    }

    private void verifyConstructorDeclarationParams(MethodDeclarationParameters params) {
        if (!params.isConstructor()) {
            throw new IllegalArgumentException("Must specify a class name for ConstructorDeclarationParams");
        }
        verifyArgumentTypesAndNames(params.getArgumentTypes(), params.getArguments());
    }

    public JavaFileWriter writeStatement(Expression statement) throws IOException {
        indent();
        statement.writeExpression(this);
        out.append(";").append("\n");
        return this;
    }
    
    public JavaFileWriter writeExpression(Expression expression) throws IOException {
        indent();
        expression.writeExpression(this);
        return this;
    }
    
    public JavaFileWriter writeAnnotation(DeclaredTypeName annotationClass) throws IOException {
        indent();
        out.append("@").append(shortenName(annotationClass, false)).append("\n");
        return this;
    }
     
    public JavaFileWriter writeStringStatement(String statement) throws IOException {
        indent();
        appendString(statement);
        out.append(";").append("\n");
        return this;
    }
    
    public JavaFileWriter appendExpression(Expression expression) throws IOException {
        expression.writeExpression(this);
        return this;
    }
    
    public JavaFileWriter appendString(String string) throws IOException {
        out.append(string);
        return this;
    }
    
    public JavaFileWriter writeNewline() throws IOException {
        out.append("\n");
        return this;
    }

    public JavaFileWriter writeComment(String comment) throws IOException {
        indent();
        out.append("// ").append(comment).append("\n");
        return this;
    }
    
    public JavaFileWriter finishMethodDefinition() throws IOException {
        finishScope(Scope.METHOD_DEFINITION);
        indent();
        out.append("}\n\n");
        return this;
    }
    
    public JavaFileWriter finishInitializerBlock(boolean semicolon, boolean newline) throws IOException {
        finishScope(Scope.METHOD_DEFINITION);
        indent();
        out.append("}");
        if (semicolon) {
            out.append(";");
        }
        if (newline) {
            out.append("\n");
        }
        return this;
    }

    public JavaFileWriter finishTypeDefinition() throws IOException {
        finishScope(Scope.TYPE_DEFINITION);
        indent();
        out.append("}\n");
        return this;
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
            if (typeName.isJavaLangPackage()) {
                simple = true;
            } else if (allNames == null || allNames.size() == 0) {
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
    
    public String shortenNameForStaticReference(TypeName name) {
        String shortenedName = shortenName(name, false);
        return shortenedName.replaceAll("<.*>", "");
    }

    public Scope getCurrentScope() {
        return scopeStack.peek();
    }

    public void checkScope(Scope... legalScopes) {
        Scope currentScope = getCurrentScope();
        if (legalScopes == null || legalScopes.length == 0) {
            throw new IllegalArgumentException("Must specify at least one legal scope");
        }
        for (Scope s : legalScopes) {
            if (currentScope == s) return;
        }
        throw new IllegalStateException("Expected one of scopes " + legalScopes + ", current scope " + currentScope);
    }

    public void moveToScope(Scope moveTo) {
        scopeStack.push(moveTo);
    }

    public void finishScope(Scope expectedFinishScope) {
        checkScope(expectedFinishScope);
        scopeStack.pop();
    }
}
