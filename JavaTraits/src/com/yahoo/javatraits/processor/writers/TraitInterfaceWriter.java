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
import javax.annotation.processing.FilerException;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.writer.JavaFileWriter;
import com.yahoo.annotations.writer.JavaFileWriter.Type;
import com.yahoo.annotations.writer.parameters.MethodDeclarationParameters;
import com.yahoo.annotations.writer.parameters.TypeDeclarationParameters;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.processor.utils.TraitProcessorUtils;

public class TraitInterfaceWriter extends JavaTraitsWriter<TraitElement> {

    public TraitInterfaceWriter(TraitElement element, TraitProcessorUtils utils) {
        super(element, utils);
    }

    @Override
    protected DeclaredTypeName getClassNameToGenerate() {
        return element.getInterfaceName();
    }

    @Override
    protected void gatherImports(Set<DeclaredTypeName> imports) {
        utils.accumulateImportsFromElements(imports, element.getDeclaredMethods());
    }

    protected void writeClassDefinition() throws IOException {
        TypeDeclarationParameters params = new TypeDeclarationParameters()
            .setName(element.getInterfaceName())
            .setKind(Type.INTERFACE)
            .setModifiers(Modifier.PUBLIC);

        writer.beginTypeDefinition(params);
        emitMethodDeclarations();
        writer.finishTypeDefinition();
    }

    private void emitMethodDeclarations() throws IOException {
        for (ExecutableElement exec : element.getDeclaredMethods()) {
            if (utils.isGetThis(element, exec)) {
                continue;
            } else {
                emitMethodDeclarationForExecutableElement(exec);
            }
        }
    }

    private void emitMethodDeclarationForExecutableElement(ExecutableElement exec) throws IOException {
        MethodDeclarationParameters methodDeclaration = utils.methodDeclarationParamsFromExecutableElement(exec, null, element.getSimpleName(), Modifier.PUBLIC);
        writer.beginMethodDefinition(methodDeclaration);
    }

}
