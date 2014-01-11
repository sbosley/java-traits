/**
 * Copyright 2014 Sam Bosley
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.sambosley.javatraits.processor.writers;

import java.io.IOException;
import java.io.Writer;
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
import com.sambosley.javatraits.utils.FullyQualifiedName;
import com.sambosley.javatraits.utils.Utils;

public class TraitDelegateWriter {

    private ClassWithTraits cls;
    private TraitElement traitElement;
    private Messager messager;
    private FullyQualifiedName traitDelegateClass;
    private FullyQualifiedName delegateClass;

    public TraitDelegateWriter(ClassWithTraits cls, TraitElement traitElement, Messager messager) {
        this.cls = cls;
        this.traitElement = traitElement;
        this.messager = messager;
        this.traitDelegateClass = cls.getDelegateClassNameForTraitElement(traitElement);
        this.delegateClass = cls.getFullyQualifiedGeneratedSuperclassName();
    }

    public void writeDelegate(Filer filer) {
        try {
            JavaFileObject jfo = filer.createSourceFile(traitDelegateClass.toString(), cls.getSourceElement());
            Writer writer = jfo.openWriter();
            writer.write(emitDelegate());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            messager.printMessage(Kind.ERROR, "IOException writing delegate class with delegate " + 
                    cls.getSimpleName() + " for trait", cls.getSourceElement());
        }
    }

    private String emitDelegate() {
        StringBuilder builder = new StringBuilder();
        emitPackage(builder);
        emitImports(builder);
        emitDelegateDeclaration(builder);
        return builder.toString();
    }

    private void emitPackage(StringBuilder builder) {
        builder.append("package ").append(traitDelegateClass.getPackageName()).append(";\n\n");
    }

    private void emitImports(StringBuilder builder) {
        Set<String> imports = new HashSet<String>();
        Utils.accumulateImportsFromExecutableElements(imports, traitElement.getAbstractMethods(), messager);
        imports.add(delegateClass.toString());
        for (String s : imports) {
            builder.append("import ").append(s).append(";\n");
        }
        builder.append("\n");
    }

    private void emitDelegateDeclaration(StringBuilder builder) {
        builder.append("public class ").append(traitDelegateClass.getSimpleName());
        if (traitElement.hasTypeParameters()) {
            builder.append("<");
            traitElement.emitParametrizedTypeList(builder, true);
            builder.append(">");
        }
        builder.append(" extends ").append(traitElement.getSimpleName());
        if (traitElement.hasTypeParameters()) {
            builder.append("<");
            traitElement.emitParametrizedTypeList(builder, false);
            builder.append(">");
        }
        builder.append(" {\n\n");

        emitDelegateInstance(builder);
        emitConstructor(builder);
        emitDefaultMethodImplementations(builder);
        emitDelegateMethodImplementations(builder);

        builder.append("}");
    }

    private void emitDelegateInstance(StringBuilder builder) {
        builder.append("\tprivate ").append(delegateClass.getSimpleName());
        cls.emitParametrizedTypeList(builder, traitElement, false);
        builder.append(" delegate;\n\n");
    }

    private void emitConstructor(StringBuilder builder) {
        builder.append("\tpublic ").append(traitDelegateClass.getSimpleName())
        .append("(").append(delegateClass.getSimpleName());
        cls.emitParametrizedTypeList(builder, traitElement, false);
        builder.append(" delegate) {\n")
        .append("\t\tthis.delegate = delegate;\n")
        .append("\t}\n\n");
    }
    
    private void emitDefaultMethodImplementations(StringBuilder builder) {
        List<? extends ExecutableElement> allMethods = traitElement.getDeclaredMethods();
        for (ExecutableElement exec : allMethods) {
            if (!exec.getModifiers().contains(Modifier.ABSTRACT)) {
                List<String> argNames = Utils.emitMethodSignature(builder, exec, "default__", traitElement.getSimpleName(), false, true);
                builder.append(" {\n");
                builder.append("\t\t");
                if (exec.getReturnType().getKind() != TypeKind.VOID)
                    builder.append("return ");
                builder.append("super.").append(exec.getSimpleName().toString()).append("(");
                for (int i = 0; i < argNames.size(); i++) {
                    builder.append(argNames.get(i));
                    if (i < argNames.size() - 1)
                        builder.append(", ");
                }
                builder.append(");\n")
                .append("\t}\n\n");
            }
        }
    }

    private void emitDelegateMethodImplementations(StringBuilder builder) {
        List<? extends ExecutableElement> abstractMethods = traitElement.getDeclaredMethods();
        for (ExecutableElement exec : abstractMethods) {
            List<String> argNames = Utils.emitMethodSignature(builder, exec, null, traitElement.getSimpleName(), false, false);
            builder.append(" {\n");
            builder.append("\t\t");
            if (exec.getReturnType().getKind() != TypeKind.VOID)
                builder.append("return ");
            builder.append("delegate.").append(exec.getSimpleName().toString()).append("(");
            for (int i = 0; i < argNames.size(); i++) {
                builder.append(argNames.get(i));
                if (i < argNames.size() - 1)
                    builder.append(", ");
            }
            builder.append(");\n")
            .append("\t}\n\n");
        }
    }

}
