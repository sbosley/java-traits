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
import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.yahoo.annotations.ClassName;
import com.yahoo.annotations.JavaFileWriter;
import com.yahoo.annotations.Utils;
import com.yahoo.javatraits.processor.data.ClassWithTraits;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.utils.TraitProcessorUtils;

public class TraitDelegateWriter {

    private ClassWithTraits cls;
    private TraitElement traitElement;
    private Messager messager;
    private ClassName traitDelegateClass;
    private ClassName delegateClass;
    private JavaFileWriter writer;

    public TraitDelegateWriter(ClassWithTraits cls, TraitElement traitElement, Messager messager) {
        this.cls = cls;
        this.traitElement = traitElement;
        this.messager = messager;
        this.traitDelegateClass = cls.getDelegateClassNameForTraitElement(traitElement);
        this.traitDelegateClass.setTypeArgs(traitElement.getTypeParameters());
        this.delegateClass = cls.getFullyQualifiedGeneratedSuperclassName();
        this.delegateClass.setTypeArgs(cls.getTypeParametersForDelegate(traitElement));
    }

    public void writeDelegate(Filer filer) {
        try {
            if (writer != null)
                throw new IllegalStateException("Already created source file for " + traitDelegateClass.toString());
            JavaFileObject jfo = filer.createSourceFile(traitDelegateClass.toString(), cls.getSourceElement());
            Writer out = jfo.openWriter();
            writer = new JavaFileWriter(out);
            emitDelegate();
            writer.close();
        } catch (IOException e) {
            messager.printMessage(Kind.ERROR, "IOException writing delegate class with delegate " +
                    cls.getSimpleName() + " for trait", cls.getSourceElement());
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
        Set<ClassName> imports = new HashSet<ClassName>();
        Utils.accumulateImportsFromExecutableElements(imports, traitElement.getDeclaredMethods(), messager);
        imports.add(delegateClass);
        imports.add(traitElement.getFullyQualifiedName());
        writer.writeImports(imports);
    }

    private void emitDelegateDeclaration() throws IOException {
        writer.beginTypeDeclaration(traitDelegateClass, "class", Modifier.PUBLIC);

        ClassName superclass = traitElement.getFullyQualifiedName().clone();
        superclass.setTypeArgs(traitElement.getTypeParameters());
        writer.addSuperclassToTypeDeclaration(superclass);
        writer.finishTypeDeclarationAndBeginTypeDefinition();

        emitDelegateInstance();
        emitConstructor();
        emitDefaultMethodImplementations();
        emitDelegateMethodImplementations();

        writer.finishTypeDefinitionAndCloseType();
    }

    private void emitDelegateInstance() throws IOException {
        writer.emitFieldDeclaration(delegateClass, "delegate", Modifier.PRIVATE);
    }

    private void emitConstructor() throws IOException {
        writer.beginConstructorDeclaration(traitDelegateClass.getSimpleName(), Modifier.PUBLIC);
        writer.addArgumentList(Arrays.asList(delegateClass),
                Arrays.asList("delegate"));
        writer.finishMethodDeclarationAndBeginMethodDefinition(null, false);
        writer.emitStatement("this.delegate = delegate;\n", 2);
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
            if (TraitProcessorUtils.isGetThis(exec))
                emitGetThis();
            else
                emitMethodDeclaration(exec, false, Modifier.PUBLIC);
        }
    }

    private void emitGetThis() throws IOException {
        writer.beginMethodDeclaration(TraitProcessorUtils.GET_THIS, traitElement.getInterfaceName(), Arrays.asList(Modifier.PUBLIC), null);
        writer.finishMethodDeclarationAndBeginMethodDefinition(null, false);
        writer.emitStatement("return delegate;\n", 2);
        writer.finishMethodDefinition();
    }

    private void emitMethodDeclaration(ExecutableElement exec, boolean isDefault, Modifier... modifiers) throws IOException {
        String name = isDefault ? "default__" + exec.getSimpleName().toString() : null;
        List<String> argNames = Utils.beginMethodDeclarationForExecutableElement(writer, exec, name, traitElement.getSimpleName(), false, modifiers);
        StringBuilder statement = new StringBuilder();
        if (exec.getReturnType().getKind() != TypeKind.VOID)
            statement.append("return ");
        String callTo = isDefault ? "super" : "delegate";
        statement.append(callTo).append(".").append(exec.getSimpleName().toString()).append("(");
        for (int i = 0; i < argNames.size(); i++) {
            statement.append(argNames.get(i));
            if (i < argNames.size() - 1)
                statement.append(", ");
        }
        statement.append(");\n");
        writer.emitStatement(statement.toString(), 2);
        writer.finishMethodDefinition();
    }

}
