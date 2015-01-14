/*
 * Copyright 2014 Yahoo Inc.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yahoo.javatraits.processor.writers;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.writer.JavaFileWriter.Type;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.aptutils.writer.parameters.TypeDeclarationParameters;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.processor.utils.TraitProcessorAptUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.Set;

public class TraitInterfaceWriter extends JavaTraitsWriter<TraitElement> {

    public TraitInterfaceWriter(TraitElement element, TraitProcessorAptUtils utils) {
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
