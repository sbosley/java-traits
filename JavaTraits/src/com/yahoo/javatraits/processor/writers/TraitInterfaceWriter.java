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
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.yahoo.annotations.model.ClassName;
import com.yahoo.annotations.utils.Utils;
import com.yahoo.annotations.writer.JavaFileWriter;
import com.yahoo.annotations.writer.JavaFileWriter.Type;
import com.yahoo.annotations.writer.JavaFileWriter.TypeDeclarationParameters;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.processor.utils.TraitProcessorUtils;

public class TraitInterfaceWriter {

    private final TraitElement element;
    private Utils utils;
    private JavaFileWriter writer = null;

    public TraitInterfaceWriter(TraitElement element, Utils utils) {
        this.element = element;
        this.utils = utils;
    }

    public void writeInterface(Filer filer) {
        try {
            if (writer != null) {
                throw new IllegalStateException("Already created source file for " + element.getInterfaceName());
            }
            JavaFileObject jfo = filer.createSourceFile(element.getInterfaceName().toString(), element.getSourceElement());
            Writer out = jfo.openWriter();
            writer = new JavaFileWriter(out);
            emitInterface();
            writer.close();
        } catch (IOException e) {
            utils.getMessager().printMessage(Kind.ERROR, "IOException writing interface for trait", element.getSourceElement());
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
        utils.accumulateImportsFromExecutableElements(imports, element.getDeclaredMethods());
        writer.writeImports(imports);
    }

    private void emitInterfaceDeclaration() throws IOException {
        TypeDeclarationParameters params = new TypeDeclarationParameters();
        params.name = element.getInterfaceName();
        params.kind = Type.INTERFACE;
        params.modifiers = Arrays.asList(Modifier.PUBLIC);

        writer.beginTypeDefinition(params);
        emitMethodDeclarations();
        writer.finishTypeDefinition();
    }

    private void emitMethodDeclarations() throws IOException {
        for (ExecutableElement exec : element.getDeclaredMethods()) {
            if (TraitProcessorUtils.isGetThis(exec)) {
                continue;
            } else {
                emitMethodDeclarationForExecutableElement(exec);
            }
        }
    }

    private void emitMethodDeclarationForExecutableElement(ExecutableElement exec) throws IOException {
        utils.beginMethodDeclarationForExecutableElement(writer, exec, null, element.getSimpleName(), Modifier.PUBLIC);
    }

}
