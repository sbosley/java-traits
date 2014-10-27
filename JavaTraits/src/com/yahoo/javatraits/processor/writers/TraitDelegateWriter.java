/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.writers;

import java.io.IOException;
import java.io.Writer;
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
import com.yahoo.annotations.writer.expressions.Expression;
import com.yahoo.annotations.writer.expressions.Expressions;
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
            .setModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .setSuperclass(superclass);

        writer.beginTypeDefinition(params);

        emitDelegateInstance();
        emitConstructor();
        emitDefaultMethodImplementations();
        emitDelegateMethodImplementations();

        writer.finishTypeDefinition();
    }

    private void emitDelegateInstance() throws IOException {
        writer.writeFieldDeclaration(delegateClass, "delegate", null, Modifier.PRIVATE);
    }

    private void emitConstructor() throws IOException {
        MethodDeclarationParameters params = new MethodDeclarationParameters()
            .setConstructorName(traitDelegateClass)
            .setModifiers(Modifier.PUBLIC)
            .setArgumentTypes(delegateClass)
            .setArgumentNames("delegate");

        writer.beginConstructorDeclaration(params);
        writer.writeStatement(Expressions.assign(Expressions.reference("this", "delegate"), Expressions.reference("delegate")));
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
            .setModifiers(Modifier.PUBLIC);

        writer.beginMethodDefinition(params);
        writer.writeStatement(Expressions.reference("delegate").returnExpr());
        writer.finishMethodDefinition();
    }

    private void emitMethodDeclaration(ExecutableElement exec, boolean isDefault, Modifier... modifiers) throws IOException {
        String name = isDefault ? "default__" + exec.getSimpleName().toString() : null;
        MethodDeclarationParameters methodDeclaration = utils.methodDeclarationParamsFromExecutableElement(exec, name, traitElement.getSimpleName(), modifiers);
        writer.beginMethodDefinition(methodDeclaration);
        
        String callTo = isDefault ? "super" : "delegate";
        Expression methodInvocation = Expressions.callMethod(callTo, exec.getSimpleName().toString(), methodDeclaration.getArguments());
        
        if (exec.getReturnType().getKind() != TypeKind.VOID) {
            methodInvocation = methodInvocation.returnExpr();
        }
        writer.writeStatement(methodInvocation);
        writer.finishMethodDefinition();
    }

}
