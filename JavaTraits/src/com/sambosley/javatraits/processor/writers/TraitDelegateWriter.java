/**
 * Copyright 2014 Yahoo Inc.
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.sambosley.javatraits.processor.writers;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
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

import com.sambosley.javatraits.processor.data.ClassWithTraits;
import com.sambosley.javatraits.processor.data.TraitElement;
import com.sambosley.javatraits.utils.ClassName;
import com.sambosley.javatraits.utils.JavaFileWriter;
import com.sambosley.javatraits.utils.TypeName;
import com.sambosley.javatraits.utils.Utils;

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
        this.delegateClass = cls.getFullyQualifiedGeneratedSuperclassName();
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
        Utils.accumulateImportsFromExecutableElements(imports, traitElement.getAbstractMethods(), messager);
        imports.add(delegateClass);
        writer.writeImports(imports);
    }

    private void emitDelegateDeclaration() throws IOException {
        writer.beginTypeDeclaration(traitDelegateClass.getSimpleName(), "class", Modifier.PUBLIC);
        if (traitElement.hasTypeParameters()) {
            writer.appendGenericDeclaration(traitElement.getTypeParameters());
        }
        writer.addSuperclassToTypeDeclaration(traitElement.getFullyQualifiedName(), traitElement.getTypeParameters());
        writer.finishTypeDeclarationAndBeginTypeDefinition();

        emitDelegateInstance();
        emitConstructor();
        emitDefaultMethodImplementations();
        emitDelegateMethodImplementations();

        writer.finishTypeDefinitionAndCloseType();
    }

    private void emitDelegateInstance() throws IOException {
        List<TypeName> generics = cls.getTypeParametersForDelegate(traitElement);
        writer.emitFieldDeclaration(delegateClass, "delegate", generics, Modifier.PRIVATE);
    }

    private void emitConstructor() throws IOException {
        writer.beginConstructorDeclaration(traitDelegateClass.getSimpleName(), Modifier.PUBLIC);
        List<TypeName> generics = cls.getTypeParametersForDelegate(traitElement);
        List<List<? extends TypeName>> genericsForArgs = new ArrayList<List<? extends TypeName>>();
        genericsForArgs.add(generics);
        writer.addArgumentList(Arrays.asList(delegateClass), 
                genericsForArgs,
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
        List<? extends ExecutableElement> abstractMethods = traitElement.getDeclaredMethods();
        for (ExecutableElement exec : abstractMethods) {
            emitMethodDeclaration(exec, false, Modifier.PUBLIC);
        }
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
