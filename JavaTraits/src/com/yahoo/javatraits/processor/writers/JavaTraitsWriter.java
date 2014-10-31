package com.yahoo.javatraits.processor.writers;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.writer.JavaFileWriter;
import com.yahoo.javatraits.processor.data.TypeElementWrapper;
import com.yahoo.javatraits.processor.utils.TraitProcessorUtils;

import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Sam on 10/31/14.
 */
public abstract class JavaTraitsWriter<T extends TypeElementWrapper> {

    protected final T element;
    protected final TraitProcessorUtils utils;
    protected JavaFileWriter writer;

    public JavaTraitsWriter(T element, TraitProcessorUtils utils) {
        this.element = element;
        this.utils = utils;
    }

    public void writeClass(Filer filer) {
        try {
            if (writer != null) {
                throw new IllegalStateException("Already created source file for " + getClassNameToGenerate());
            }
            JavaFileObject jfo = filer.createSourceFile(getClassNameToGenerate().toString(), element.getSourceElement());
            Writer out = jfo.openWriter();
            writer = new JavaFileWriter(out);
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
