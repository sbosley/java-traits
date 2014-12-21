/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.aptutils.writer;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.GenericName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.model.TypeName.TypeNameVisitor;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.aptutils.writer.parameters.TypeDeclarationParameters;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * Class to facilitate writing Java files during annotation processing
 *
 * This class handles a few things that enable you to generate more readable/debuggable Java files:
 *
 * <ul>
 *     <li>Automatic imports handling and name shortening. Any references to type names will be
 *     shortened if they exist in the imports and are unique. Otherwise, fully qualified names
 *     will be used to prevent name conflicts.</li>
 *     <li>Automatic indentation. This file tracks the state of the java file being emitted
 *     to calculate a reasonable indent level for each statement written</li>
 *     <li>Seamlessly handles {@link com.yahoo.aptutils.writer.expressions.Expression} objects
 *     when they are encountered to make writing Java expressions easy</li>
 * </ul>
 */
public class JavaFileWriter {

    private static final String INDENT = "    ";

    private Writer out;
    private String packageName;
    private Map<String, List<DeclaredTypeName>> knownNames;
    private Type kind = null;
    private Deque<Scope> scopeStack = new LinkedList<Scope>();

    /**
     * Enum for the kind of object definition to write. Currently only
     * class and interface are supported.
     */
    public static enum Type {
        CLASS("class"),
        INTERFACE("interface");

        private String name;
        private Type(String name) {
            this.name = name;
        }
    }

    /**
     * Enum for the different states of the writer
     */
    public static enum Scope {
        PACKAGE,
        IMPORTS,
        TYPE_DEFINITION,
        METHOD_DEFINITION,
    }

    /**
     * @param out writer object to output to. When writing an annotation processor, the writer object is usually obtained
     *            like this:
     *            <br/>
     *            JavaFileObject jfo = filer.createSourceFile(classNameToGenerate, originatingElement);
                  JavaFileWriter writer = new JavaFileWriter(jfo.openWriter());
     */
    public JavaFileWriter(Writer out) {
        if (out == null) {
            throw new IllegalArgumentException("Writer must be non-null");
        }
        this.out = out;
        this.knownNames = new HashMap<String, List<DeclaredTypeName>>();
        scopeStack.push(Scope.PACKAGE);
    }

    /**
     * Ends the writing of this file and closes the output stream
     *
     * @return this
     * @throws IOException
     */
    public JavaFileWriter close() throws IOException {
        out.close();
        return this;
    }

    /**
     * Writes the package declaration. Should probably only ever be called as the first thing you
     * do after opening the writer
     *
     * @param packageName
     * @return this
     * @throws IOException
     */
    public JavaFileWriter writePackage(String packageName) throws IOException {
        checkScope(Scope.PACKAGE);
        out.append("package ").append(packageName).append(";\n\n");
        this.packageName = packageName;
        finishScope(Scope.PACKAGE);
        moveToScope(Scope.IMPORTS);
        return this;
    }

    /**
     * Write import statements for the classes in this collection. Also registers known class names to
     * facilitate name shortening
     *
     * @param imports classes to be imported
     * @return this
     * @throws IOException
     */
    public JavaFileWriter writeImports(Collection<DeclaredTypeName> imports) throws IOException {
        checkScope(Scope.IMPORTS);
        TreeSet<String> sortedImports = new TreeSet<String>();
        if (!AptUtils.isEmpty(imports)) {
            for (DeclaredTypeName item : imports) {
                DeclaredTypeName toImport = addToKnownNames(item, item.isJavaLangPackage() || item.getPackageName().equals(packageName));
                if (toImport != null) {
                    sortedImports.add(toImport.toString());
                }
            }
        }
        if (!AptUtils.isEmpty(sortedImports)) {
            for (String item : sortedImports) {
                out.append("import ").append(item).append(";\n");
            }
            out.append("\n");
        }
        finishScope(Scope.IMPORTS);
        return this;
    }

    /**
     * Registers class names that can be shortened (i.e. not fully qualified) but don't need to be imported, e.g.
     * classes in the same package as the generated class
     *
     * @param otherKnownNames class names that should be known to the file
     * @return this
     * @throws IOException
     */
    public JavaFileWriter registerOtherKnownNames(Collection<DeclaredTypeName> otherKnownNames) throws IOException {
        if (!AptUtils.isEmpty(otherKnownNames)) {
            for (DeclaredTypeName item : otherKnownNames) {
                addToKnownNames(item, false);
            }
        }
        return this;
    }

    /**
     * Registers class names that can be shortened (i.e. not fully qualified) but don't need to be imported, e.g.
     * classes in the same package as the generated class
     *
     * @param otherKnownNames class names that should be known to the file
     * @return this
     * @throws IOException
     */
    public JavaFileWriter registerOtherKnownNames(DeclaredTypeName... otherKnownNames) throws IOException {
        return registerOtherKnownNames(AptUtils.asList(otherKnownNames));
    }
    
    // Returns a type name that needs to be added to the imports
    private DeclaredTypeName addToKnownNames(DeclaredTypeName type, boolean highestPreference) {
        String simpleName = type.getSimpleName();
        List<DeclaredTypeName> allNames = knownNames.get(simpleName);
        if (allNames == null) {
            allNames = new ArrayList<DeclaredTypeName>();
            knownNames.put(simpleName, allNames);
        }

        if (!allNames.contains(type)) {
            if (highestPreference) {
                DeclaredTypeName toReturn = allNames.size() > 0 ? allNames.get(0) : null;
                allNames.add(0, type);
                return toReturn;
            } else {
                allNames.add(type);
                return type;
            }
        }
        return null;
    }

    /**
     * Begins a new type definition.
     *
     * @param typeDeclaration see {@link com.yahoo.aptutils.writer.parameters.TypeDeclarationParameters}
     * @return this
     * @throws IOException
     */
    public JavaFileWriter beginTypeDefinition(TypeDeclarationParameters typeDeclaration) throws IOException {
        validateTypeDeclarationParams(typeDeclaration);
        indent();

        boolean isRootClass = AptUtils.isEmpty(scopeStack);
        if (!isRootClass) {
            checkScope(Scope.TYPE_DEFINITION); // Begin a new inner type definition 
        } else {
            addToKnownNames(typeDeclaration.getClassName(), true);
        }
        
        this.kind = typeDeclaration.getKind();
        writeModifierList(typeDeclaration.getModifiers());
        out.append(typeDeclaration.getKind().name).append(" ").append(typeDeclaration.getClassName().getSimpleName());
        writeGenericsList(typeDeclaration.getClassName().getTypeArgs(), true);

        if (kind == Type.CLASS && typeDeclaration.getSuperclass() != null && !CoreTypes.JAVA_OBJECT.equals(typeDeclaration.getSuperclass())) {
            out.append(" extends ").append(shortenName(typeDeclaration.getSuperclass(), false));
        }

        if (!AptUtils.isEmpty(typeDeclaration.getInterfaces())) {
            if (kind == Type.INTERFACE) {
                out.append(" extends ");
            } else {
                out.append(" implements ");
            }
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

    /**
     * Writes a field declaration
     *
     * @param type the type for the field
     * @param name the name for the field
     * @param initializer expression with which to initialize the field. If null, the field will have no initial value
     * @param modifiers access modifiers for the field
     * @return this
     * @throws IOException
     */
    public JavaFileWriter writeFieldDeclaration(TypeName type, String name, Expression initializer, Modifier... modifiers) throws IOException {
        return writeFieldDeclaration(type, name, initializer, AptUtils.asList(modifiers));
    }

    /**
     * Writes a field declaration
     *
     * @param type the type for the field
     * @param name the name for the field
     * @param initializer expression with which to initialize the field. If null, the field will have no initial value
     * @param modifiers access modifiers for the field
     * @return this
     * @throws IOException
     */
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

    /**
     * Begins a method definition
     *
     * @param methodDeclaration see {@link com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters}
     * @return this
     * @throws IOException
     */
    public JavaFileWriter beginMethodDefinition(MethodDeclarationParameters methodDeclaration) throws IOException {
        validateMethodDefinitionParams(methodDeclaration);
        checkScope(Scope.TYPE_DEFINITION);
        indent();
        boolean isAbstract = kind.equals(Type.INTERFACE) ||
                (!AptUtils.isEmpty(methodDeclaration.getModifiers())
                        && methodDeclaration.getModifiers().contains(Modifier.ABSTRACT));
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
        if (!AptUtils.isEmpty(methodDeclaration.getThrowsTypes())) {
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

    /**
     * Begins an initializer block
     *
     * @see com.yahoo.aptutils.writer.expressions.Expressions#block(com.yahoo.aptutils.writer.expressions.Expression, boolean, boolean, boolean, boolean)
     *
     * @param isStatic true if the block is static
     * @param indentStart true if the block needs to be indented, i.e. it's on its own line and not for example
     *                    the start of an array constant declaration
     * @return this
     * @throws IOException
     */
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
        if (AptUtils.isEmpty(params.getMethodName())) {
            throw new IllegalArgumentException("Must specify a method name for MethodDeclarationParams");
        }
        verifyArgumentTypesAndNames(params.getArgumentTypes(), params.getArgumentNames());
    }

    private void verifyArgumentTypesAndNames(List<? extends TypeName> argumentTypes, List<?> arguments) {
        if (AptUtils.isEmpty(argumentTypes) && !AptUtils.isEmpty(arguments)) {
            throw new IllegalArgumentException("Must specify argument types for MethodDeclarationParams");
        }
        if (!AptUtils.isEmpty(argumentTypes) && AptUtils.isEmpty(arguments)) {
            throw new IllegalArgumentException("Must specify argument names for MethodDeclarationParams");
        }
        if (!AptUtils.isEmpty(argumentTypes) && !AptUtils.isEmpty(arguments)
                && argumentTypes.size() != arguments.size()) {
            String error = "Different number of argument types and names in MethodDeclarationParams. "
                    + argumentTypes.size() + " types, " + arguments.size() + " names.";
            throw new IllegalArgumentException(error);
        }
    }

    /**
     * Write a comma-separated list of argument types and names
     * <br/>
     * E.g. Type1 name1, Type2 name2
     *
     * @param argumentTypes
     * @param argumentNames
     * @return this
     * @throws IOException
     */
    public JavaFileWriter writeArgumentList(List<? extends TypeName> argumentTypes, List<?> argumentNames) throws IOException {
        out.append("(");
        if (argumentNames != null) {
            for (int i = 0; i < argumentNames.size(); i++) {
                TypeName argType = argumentTypes != null ? argumentTypes.get(i) : null;
                
                if (argType != null) {
                    out.append(shortenName(argType, false)).append(" ");
                }
                
                Object argument = argumentNames.get(i);
                if (argument instanceof Expression) {
                    ((Expression) argument).writeExpression(this);
                } else {
                    out.append(String.valueOf(argument));
                }
                if (i < argumentNames.size() - 1) {
                    out.append(", ");
                }
            }
        }
        out.append(")");
        return this;
    }

    /**
     * Write a comma-separated list of argument names
     * <br/>
     * E.g. name1, name2
     *
     * @param argumentNames
     * @return this
     * @throws IOException
     */
    public JavaFileWriter writeArgumentNameList(List<?> argumentNames) throws IOException {
        return writeArgumentList(null, argumentNames);
    }

    /**
     * Begin a constructor declaration. Special case of {@link #beginMethodDefinition(com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters)}
     * where the params have a constructor name rather than a method name set
     *
     * @param constructorDeclaration
     * @return this
     * @throws IOException
     */
    public JavaFileWriter beginConstructorDeclaration(MethodDeclarationParameters constructorDeclaration) throws IOException {
        verifyConstructorDeclarationParams(constructorDeclaration);
        checkScope(Scope.TYPE_DEFINITION);
        indent();
        writeModifierList(constructorDeclaration.getModifiers());
        out.append(constructorDeclaration.getConstructorName().getSimpleName());
        writeGenericsList(constructorDeclaration.getMethodGenerics(), false);
        writeArgumentList(constructorDeclaration.getArgumentTypes(), constructorDeclaration.getArgumentNames());
        out.append(" {\n");
        moveToScope(Scope.METHOD_DEFINITION);
        return this;
    }

    private void verifyConstructorDeclarationParams(MethodDeclarationParameters params) {
        if (!params.isConstructor()) {
            throw new IllegalArgumentException("Must specify a class name for ConstructorDeclarationParams");
        }
        verifyArgumentTypesAndNames(params.getArgumentTypes(), params.getArgumentNames());
    }

    /**
     * Writes an expression as a statement, ending the line with a semicolon and a \n. Indents before writing the expression
     *
     * @param statement
     * @return this
     * @throws IOException
     */
    public JavaFileWriter writeStatement(Expression statement) throws IOException {
        indent();
        statement.writeExpression(this);
        out.append(";").append("\n");
        return this;
    }

    /**
     * The same as {@link #writeStatement(com.yahoo.aptutils.writer.expressions.Expression)} except does not
     * append the semicolon or \n
     *
     * @param expression
     * @return this
     * @throws IOException
     */
    public JavaFileWriter writeExpression(Expression expression) throws IOException {
        indent();
        expression.writeExpression(this);
        return this;
    }

    /**
     * Writes an annotation
     *
     * @param annotationClass
     * @return
     * @throws IOException
     */
    public JavaFileWriter writeAnnotation(DeclaredTypeName annotationClass) throws IOException {
        indent();
        out.append("@").append(shortenName(annotationClass, false)).append("\n");
        return this;
    }

    /**
     * Similar to {@link #writeStatement(com.yahoo.aptutils.writer.expressions.Expression)}
     *
     * @param statement
     * @return this
     * @throws IOException
     */
    public JavaFileWriter writeStringStatement(String statement) throws IOException {
        indent();
        appendString(statement);
        out.append(";").append("\n");
        return this;
    }

    /**
     * Directly appends the expression with no indenting
     *
     * @param expression
     * @return this
     * @throws IOException
     */
    public JavaFileWriter appendExpression(Expression expression) throws IOException {
        expression.writeExpression(this);
        return this;
    }

    /**
     * Directly appends the string with no indenting
     *
     * @param string
     * @return
     * @throws IOException
     */
    public JavaFileWriter appendString(String string) throws IOException {
        out.append(string);
        return this;
    }

    /**
     * Writes a newline (\n)
     *
     * @return
     * @throws IOException
     */
    public JavaFileWriter writeNewline() throws IOException {
        out.append("\n");
        return this;
    }

    /**
     * Writes a comment. Indents and then outputs "// <comment>\n"
     *
     * @param comment
     * @return this
     * @throws IOException
     */
    public JavaFileWriter writeComment(String comment) throws IOException {
        indent();
        out.append("// ").append(comment).append("\n");
        return this;
    }

    /**
     * Finishes a method definition
     *
     * @return this
     * @throws IOException
     */
    public JavaFileWriter finishMethodDefinition() throws IOException {
        finishScope(Scope.METHOD_DEFINITION);
        indent();
        out.append("}\n\n");
        return this;
    }

    /**
     * Finshes an initializer block
     *
     * @param semicolon true if the block should be followed by a semicolon
     * @param newline true if the block should be followed by a newline (\n)
     * @return this
     * @throws IOException
     */
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

    /**
     * Finish a type (i.e. class or interface) definition
     *
     * @return this
     * @throws IOException
     */
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

    private String getGenericsListString(List<? extends TypeName> generics, boolean includeBounds) {
        if (AptUtils.isEmpty(generics)) {
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

    /**
     * Returns the shortened version of this type name given the current state of the writer (e.g known imports
     * and other names).
     *
     * @param name name to shorted
     * @param includeGenericBounds true if any generics should include their extends or super bounds
     * @return this
     */
    public String shortenName(TypeName name, boolean includeGenericBounds) {
        return name.accept(nameShorteningVisitor, includeGenericBounds);
    }

    /**
     * Shortens a name for a static reference
     *
     * @param name name to be shortened
     * @return this
     */
    public String shortenNameForStaticReference(TypeName name) {
        String shortenedName = shortenName(name, false);
        return shortenedName.replaceAll("<.*>", "");
    }

    /**
     * @return the current scope of the writer
     */
    public Scope getCurrentScope() {
        return scopeStack.peek();
    }

    /**
     * Check the current scope of the writer against the expected legalScopes
     *
     * @param legalScopes
     * @throws java.lang.IllegalStateException if the current scope is not
     * one of the expected scopes
     */
    public void checkScope(Scope... legalScopes) {
        Scope currentScope = getCurrentScope();
        if ((legalScopes == null || legalScopes.length == 0) && currentScope != null) {
            throw new IllegalStateException("Expected no scope but was " + currentScope);
        }
        for (Scope s : legalScopes) {
            if (currentScope == s) return;
        }
        throw new IllegalStateException("Expected one of scopes " + legalScopes + ", current scope " + currentScope);
    }

    /**
     * Move to a new scope
     * @param moveTo
     */
    public void moveToScope(Scope moveTo) {
        scopeStack.push(moveTo);
    }

    /**
     * Validates the current expected scope and finishes it
     *
     * @param expectedFinishScope
     */
    public void finishScope(Scope expectedFinishScope) {
        checkScope(expectedFinishScope);
        scopeStack.pop();
    }
}
