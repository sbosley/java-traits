package com.yahoo.javatraits.processor.writers;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.writer.JavaFileWriter;
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
