/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.writers;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.writer.JavaFileWriter.Type;
import com.yahoo.annotations.writer.parameters.MethodDeclarationParameters;
import com.yahoo.annotations.writer.parameters.TypeDeclarationParameters;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.processor.utils.TraitProcessorUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.Set;

public class TraitInterfaceWriter extends JavaTraitsWriter<TraitElement> {

    public TraitInterfaceWriter(TraitElement element, TraitProcessorUtils utils) {
        super(element, utils);
    }

    @Override
    protected DeclaredTypeName getClassNameToGenerate() {
        return element.getGeneratedInterfaceName();
    }

    @Override
    protected void gatherImports(Set<DeclaredTypeName> imports) {
        utils.accumulateImportsFromTypeNames(imports, element.getInterfaceNames());
        utils.accumulateImportsFromElements(imports, element.getDeclaredMethods());
    }

    protected void writeClassDefinition() throws IOException {
        TypeDeclarationParameters params = new TypeDeclarationParameters()
            .setName(element.getGeneratedInterfaceName())
            .setKind(Type.INTERFACE)
            .setInterfaces(element.getInterfaceNames())
            .setModifiers(Modifier.PUBLIC);

        writer.beginTypeDefinition(params);
        emitMethodDeclarations();
        writer.finishTypeDefinition();
    }

    private void emitMethodDeclarations() throws IOException {
        for (ExecutableElement exec : element.getDeclaredMethods()) {
            if (!utils.isGetThis(element, exec)) {
                emitMethodDeclarationForExecutableElement(exec);
            }
        }
    }

    private void emitMethodDeclarationForExecutableElement(ExecutableElement exec) throws IOException {
        MethodDeclarationParameters methodDeclaration = utils.methodDeclarationParamsFromExecutableElement(exec, null, element.getSimpleName(), Modifier.PUBLIC);
        writer.beginMethodDefinition(methodDeclaration);
    }

}
