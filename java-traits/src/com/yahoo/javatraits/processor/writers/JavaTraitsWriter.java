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
import com.yahoo.aptutils.writer.JavaFileWriter;
import com.yahoo.javatraits.processor.data.TypeElementWrapper;
import com.yahoo.javatraits.processor.utils.TraitProcessorAptUtils;

import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public abstract class JavaTraitsWriter<T extends TypeElementWrapper> {

    protected final T element;
    protected final TraitProcessorAptUtils utils;
    protected JavaFileWriter writer;

    public JavaTraitsWriter(T element, TraitProcessorAptUtils utils) {
        this.element = element;
        this.utils = utils;
    }

    public void writeClass(Filer filer) {
        try {
            if (writer != null) {
                throw new IllegalStateException("Already created source file for " + getClassNameToGenerate());
            }
            JavaFileObject jfo = filer.createSourceFile(getClassNameToGenerate().toString(), element.getSourceElement());
            writer = new JavaFileWriter(jfo.openWriter());
            writeFile();
            writer.close();
        } catch (FilerException e) {
            utils.getMessager().printMessage(Kind.ERROR, "FilerException creating file " + getClassNameToGenerate() + ": " + e.getMessage(), element.getSourceElement());
        } catch (IOException e) {
            utils.getMessager().printMessage(Kind.ERROR, "IOException writing file " + getClassNameToGenerate() + ": " + e.getMessage(), element.getSourceElement());
        }
    }

    protected abstract DeclaredTypeName getClassNameToGenerate();

    private void writeFile() throws IOException {
        writePackage();
        writeImports();
        writeClassDefinition();
    }

    private void writePackage() throws IOException {
        writer.writePackage(element.getPackageName());
    }

    private void writeImports() throws IOException {
        Set<DeclaredTypeName> imports = new HashSet<DeclaredTypeName>();
        gatherImports(imports);
        writer.writeImports(imports);
    }

    protected abstract void gatherImports(Set<DeclaredTypeName> imports);
    protected abstract void writeClassDefinition() throws IOException;

}
