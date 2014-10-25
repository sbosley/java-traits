/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.writers;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.writer.JavaFileWriter;
import com.yahoo.annotations.writer.JavaFileWriter.Type;
import com.yahoo.annotations.writer.parameters.MethodDeclarationParameters;
import com.yahoo.annotations.writer.parameters.TypeDeclarationParameters;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.processor.utils.TraitProcessorUtils;

public class TraitDelegateWriter {

    private TraitElement traitElement;
    private TraitProcessorUtils utils;
    private DeclaredTypeName traitDelegateClass;
    private DeclaredTypeName delegateClass;
    private JavaFileWriter writer;

    public TraitDelegateWriter(TraitElement traitElement, TraitProcessorUtils utils) {
        this.traitElement = traitElement;
        this.utils = utils;
        this.traitDelegateClass = traitElement.getDelegateName();
        this.delegateClass = traitElement.getInterfaceName();
    }

    public void writeDelegate(Filer filer) {
        try {
            if (writer != null) {
                throw new IllegalStateException("Already created source file for " + traitDelegateClass.toString());
            }
            JavaFileObject jfo = filer.createSourceFile(traitDelegateClass.toString(), traitElement.getSourceElement());
            Writer out = jfo.openWriter();
            writer = new JavaFileWriter(out);
            emitDelegate();
            writer.close();
        } catch (IOException e) {
            utils.getMessager().printMessage(Kind.ERROR, "IOException writing delegate class for trait", traitElement.getSourceElement());
        }
    }

    private void emitDelegate() throws IOException {
        emitPackage();
        emitImports();
        emitDelegateDeclaration();
    }

    private void emitPackage() throws IOException {
        writer.writePackage(traitDelegateClass.getPackageName());
    }

    private void emitImports() throws IOException {
        Set<DeclaredTypeName> imports = new HashSet<DeclaredTypeName>();
        utils.accumulateImportsFromExecutableElements(imports, traitElement.getDeclaredMethods());
        for (ExecutableElement e : traitElement.getDeclaredMethods()) {
            if (utils.isGetThis(traitElement, e)) {
                imports.add(traitElement.getInterfaceName());
            }
        }
        imports.add(delegateClass);
        imports.add(traitElement.getFullyQualifiedName());
        writer.writeImports(imports);
    }

    private void emitDelegateDeclaration() throws IOException {
        DeclaredTypeName superclass = traitElement.getFullyQualifiedName().clone();
        superclass.setTypeArgs(traitElement.getTypeParameters());
        
        TypeDeclarationParameters params = new TypeDeclarationParameters()
            .setName(traitDelegateClass)
            .setKind(Type.CLASS)
            .setModifiers(Arrays.asList(Modifier.PUBLIC, Modifier.FINAL))
            .setSuperclass(superclass);

        writer.beginTypeDefinition(params);

        emitDelegateInstance();
        emitConstructor();
        emitDefaultMethodImplementations();
        emitDelegateMethodImplementations();

        writer.finishTypeDefinition();
    }

    private void emitDelegateInstance() throws IOException {
        writer.writeFieldDeclaration(delegateClass, "delegate", Arrays.asList(Modifier.PRIVATE), null);
    }

    private void emitConstructor() throws IOException {
        MethodDeclarationParameters params = new MethodDeclarationParameters()
            .setConstructorName(traitDelegateClass)
            .setModifiers(Arrays.asList(Modifier.PUBLIC))
            .setArgumentTypes(Arrays.asList(delegateClass))
            .setArgumentNames(Arrays.asList("delegate"));

        writer.beginConstructorDeclaration(params);
        writer.writeMethodBodyStatement("this.delegate = delegate;\n");
        writer.finishMethodDefinition();
    }

    private void emitDefaultMethodImplementations() throws IOException {
        List<? extends ExecutableElement> allMethods = traitElement.getDeclaredMethods();
        for (ExecutableElement exec : allMethods) {
            if (!exec.getModifiers().contains(Modifier.ABSTRACT)) {
                emitMethodDeclaration(exec, true, Modifier.PUBLIC, Modifier.FINAL);
            }
        }
    }

    private void emitDelegateMethodImplementations() throws IOException {
        List<? extends ExecutableElement> allMethods = traitElement.getDeclaredMethods();
        for (ExecutableElement exec : allMethods) {
            if (utils.isGetThis(traitElement, exec)) {
                emitGetThis();
            } else {
                emitMethodDeclaration(exec, false, Modifier.PUBLIC);
            }
        }
    }

    private void emitGetThis() throws IOException {
        MethodDeclarationParameters params = new MethodDeclarationParameters()
            .setMethodName(TraitProcessorUtils.GET_THIS)
            .setReturnType(delegateClass)
            .setModifiers(Arrays.asList(Modifier.PUBLIC));

        writer.beginMethodDefinition(params);
        writer.writeMethodBodyStatement("return delegate;\n");
        writer.finishMethodDefinition();
    }

    private void emitMethodDeclaration(ExecutableElement exec, boolean isDefault, Modifier... modifiers) throws IOException {
        String name = isDefault ? "default__" + exec.getSimpleName().toString() : null;
        MethodDeclarationParameters methodDeclaration = utils.methodDeclarationParamsFromExecutableElement(exec, name, traitElement.getSimpleName(), modifiers);
        writer.beginMethodDefinition(methodDeclaration);
        
        StringBuilder statement = new StringBuilder();
        if (exec.getReturnType().getKind() != TypeKind.VOID) {
            statement.append("return ");
        }
        String callTo = isDefault ? "super" : "delegate";
        statement.append(callTo).append(".").append(exec.getSimpleName().toString()).append("(");
        
        List<String> argNames = methodDeclaration.getArgumentNames();
        for (int i = 0; i < argNames.size(); i++) {
            statement.append(argNames.get(i));
            if (i < argNames.size() - 1) {
                statement.append(", ");
            }
        }
        statement.append(");\n");
        writer.writeMethodBodyStatement(statement.toString());
        writer.finishMethodDefinition();
    }

}
