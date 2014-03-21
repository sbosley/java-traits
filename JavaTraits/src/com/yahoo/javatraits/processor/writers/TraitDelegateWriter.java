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

import com.yahoo.annotations.model.ClassName;
import com.yahoo.annotations.utils.Utils;
import com.yahoo.annotations.writer.JavaFileWriter;
import com.yahoo.annotations.writer.JavaFileWriter.ConstructorDeclarationParams;
import com.yahoo.annotations.writer.JavaFileWriter.MethodDeclarationParams;
import com.yahoo.annotations.writer.JavaFileWriter.Type;
import com.yahoo.annotations.writer.JavaFileWriter.TypeDeclarationParameters;
import com.yahoo.javatraits.processor.data.ClassWithTraits;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.processor.utils.TraitProcessorUtils;

public class TraitDelegateWriter {

    private ClassWithTraits cls;
    private TraitElement traitElement;
    private Utils utils;
    private ClassName traitDelegateClass;
    private ClassName delegateClass;
    private JavaFileWriter writer;

    public TraitDelegateWriter(ClassWithTraits cls, TraitElement traitElement, Utils utils) {
        this.cls = cls;
        this.traitElement = traitElement;
        this.utils = utils;
        this.traitDelegateClass = cls.getDelegateClassNameForTraitElement(traitElement);
        this.traitDelegateClass.setTypeArgs(traitElement.getTypeParameters());
        this.delegateClass = cls.getFullyQualifiedGeneratedSuperclassName();
        this.delegateClass.setTypeArgs(cls.getTypeParametersForDelegate(traitElement));
    }

    public void writeDelegate(Filer filer) {
        try {
            if (writer != null) {
                throw new IllegalStateException("Already created source file for " + traitDelegateClass.toString());
            }
            JavaFileObject jfo = filer.createSourceFile(traitDelegateClass.toString(), cls.getSourceElement());
            Writer out = jfo.openWriter();
            writer = new JavaFileWriter(out);
            emitDelegate();
            writer.close();
        } catch (IOException e) {
            utils.getMessager().printMessage(Kind.ERROR, "IOException writing delegate class with delegate " +
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
        utils.accumulateImportsFromExecutableElements(imports, traitElement.getDeclaredMethods());
        for (ExecutableElement e : traitElement.getDeclaredMethods()) {
            if (TraitProcessorUtils.isGetThis(e)) {
                imports.add(traitElement.getInterfaceName());
            }
        }
        imports.add(delegateClass);
        imports.add(traitElement.getFullyQualifiedName());
        writer.writeImports(imports);
    }

    private void emitDelegateDeclaration() throws IOException {
        ClassName superclass = traitElement.getFullyQualifiedName().clone();
        superclass.setTypeArgs(traitElement.getTypeParameters());
        TypeDeclarationParameters params = new TypeDeclarationParameters();
        params.name = traitDelegateClass;
        params.kind = Type.CLASS;
        params.modifiers = Arrays.asList(Modifier.PUBLIC);
        params.superclass = superclass;

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
        ConstructorDeclarationParams params = new ConstructorDeclarationParams();
        params.name = traitDelegateClass;
        params.modifiers = Arrays.asList(Modifier.PUBLIC);
        params.argumentTypes = Arrays.asList(delegateClass);
        params.argumentNames = Arrays.asList("delegate");

        writer.beginConstructorDeclaration(params);
        writer.writeStatement("this.delegate = delegate;\n", 2);
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
            if (TraitProcessorUtils.isGetThis(exec)) {
                emitGetThis();
            } else {
                emitMethodDeclaration(exec, false, Modifier.PUBLIC);
            }
        }
    }

    private void emitGetThis() throws IOException {
        MethodDeclarationParams params = new MethodDeclarationParams();
        params.name = TraitProcessorUtils.GET_THIS;
        params.returnType = traitElement.getInterfaceName();
        params.modifiers = Arrays.asList(Modifier.PUBLIC);

        writer.beginMethodDefinition(params);
        writer.writeStatement("return delegate;\n", 2);
        writer.finishMethodDefinition();
    }

    private void emitMethodDeclaration(ExecutableElement exec, boolean isDefault, Modifier... modifiers) throws IOException {
        String name = isDefault ? "default__" + exec.getSimpleName().toString() : null;
        List<String> argNames = utils.beginMethodDeclarationForExecutableElement(writer, exec, name, traitElement.getSimpleName(), modifiers);
        StringBuilder statement = new StringBuilder();
        if (exec.getReturnType().getKind() != TypeKind.VOID) {
            statement.append("return ");
        }
        String callTo = isDefault ? "super" : "delegate";
        statement.append(callTo).append(".").append(exec.getSimpleName().toString()).append("(");
        for (int i = 0; i < argNames.size(); i++) {
            statement.append(argNames.get(i));
            if (i < argNames.size() - 1) {
                statement.append(", ");
            }
        }
        statement.append(");\n");
        writer.writeStatement(statement.toString(), 2);
        writer.finishMethodDefinition();
    }

}
