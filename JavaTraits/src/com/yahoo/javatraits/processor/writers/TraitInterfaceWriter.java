/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.writers;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.yahoo.annotations.ClassName;
import com.yahoo.annotations.JavaFileWriter;
import com.yahoo.annotations.Utils;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.processor.utils.TraitProcessorUtils;

public class TraitInterfaceWriter {

    private final TraitElement element;
    private Messager messager;
    private JavaFileWriter writer = null;

    public TraitInterfaceWriter(TraitElement element, Messager messager) {
        this.element = element;
        this.messager = messager;
    }

    public void writeInterface(Filer filer) {
        try {
            if (writer != null)
                throw new IllegalStateException("Already created source file for " + element.getInterfaceName());
            JavaFileObject jfo = filer.createSourceFile(element.getInterfaceName().toString(), element.getSourceElement());
            Writer out = jfo.openWriter();
            writer = new JavaFileWriter(out);
            emitInterface();
            writer.close();
        } catch (IOException e) {
            messager.printMessage(Kind.ERROR, "IOException writing interface for trait", element.getSourceElement());
        }
    }

    private void emitInterface() throws IOException {
        emitPackage();
        emitImports();
        emitInterfaceDeclaration();
    }

    private void emitPackage() throws IOException {
        writer.writePackage(element.getPackageName());
    }

    private void emitImports() throws IOException {
        Set<ClassName> imports = new HashSet<ClassName>();
        Utils.accumulateImportsFromExecutableElements(imports, element.getDeclaredMethods(), messager);
        writer.writeImports(imports);
    }

    private void emitInterfaceDeclaration() throws IOException {
        writer.beginTypeDeclaration(element.getInterfaceName(), "interface", Modifier.PUBLIC);
        writer.finishTypeDeclarationAndBeginTypeDefinition();
        emitMethodDeclarations();
        writer.finishTypeDefinitionAndCloseType();
    }

    private void emitMethodDeclarations() throws IOException {
        for (ExecutableElement exec : element.getDeclaredMethods()) {
            if (TraitProcessorUtils.isGetThis(exec))
                continue;
            else
                emitMethodDeclarationForExecutableElement(exec);
        }
    }

    private void emitMethodDeclarationForExecutableElement(ExecutableElement exec) throws IOException {
        Utils.beginMethodDeclarationForExecutableElement(writer, exec, null, element.getSimpleName(), true, Modifier.PUBLIC);
    }

}
