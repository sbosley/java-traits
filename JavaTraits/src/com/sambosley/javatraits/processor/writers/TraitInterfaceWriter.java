/**
 * Copyright 2014 Sam Bosley
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.sambosley.javatraits.processor.writers;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.sambosley.javatraits.processor.data.TraitElement;
import com.sambosley.javatraits.utils.ClassName;
import com.sambosley.javatraits.utils.GenericName;
import com.sambosley.javatraits.utils.JavaFileWriter;
import com.sambosley.javatraits.utils.TypeName;
import com.sambosley.javatraits.utils.Utils;

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
                throw new IllegalStateException("Already created source file for " + element.getFullyQualifiedInterfaceName());
            JavaFileObject jfo = filer.createSourceFile(element.getFullyQualifiedInterfaceName(), element.getSourceElement());
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
        writer.beginTypeDeclaration(element.getSimpleInterfaceName(), "interface", Modifier.PUBLIC);
        writer.appendGenericDeclaration(element.getTypeParameters()); 
        writer.finishTypeDeclarationAndBeginTypeDefinition();
        emitMethodDeclarations();
        writer.finishTypeDefinitionAndCloseType();
    }

    private void emitMethodDeclarations() throws IOException {
        for (ExecutableElement exec : element.getDeclaredMethods()) {
            emitMethodDeclarationForExecutableElement(exec);
        }
    }

    private void emitMethodDeclarationForExecutableElement(ExecutableElement exec) throws IOException {
        String name = exec.getSimpleName().toString();
        List<TypeName> methodGenerics = Utils.mapTypeParameterElementsToTypeName(exec.getTypeParameters(), null);
        TypeName returnType = Utils.getTypeNameFromTypeMirror(exec.getReturnType(), null);
        if (!methodGenerics.contains(returnType) && returnType instanceof GenericName)
            ((GenericName) returnType).addQualifier(element.getSimpleName());
        writer.beginMethodDeclaration(name, returnType, Arrays.asList(Modifier.PUBLIC), methodGenerics);
        emitMethodArguments(exec, methodGenerics);
        List<TypeName> thrownTypes = getThrownTypes(exec, methodGenerics);
        writer.finishMethodDeclarationAndBeginMethodDefinition(thrownTypes, true);
    }
    
    private void emitMethodArguments(ExecutableElement exec, final List<TypeName> methodGenerics) throws IOException {
        List<? extends VariableElement> arguments = exec.getParameters();
        List<TypeName> typeNames = Utils.map(arguments, new Utils.MapFunction<VariableElement, TypeName>() {
            @Override
            public TypeName map(VariableElement arg) {
                return Utils.getTypeNameFromTypeMirror(arg.asType(), null);
            }
        });
        Utils.map(typeNames, new Utils.MapFunction<TypeName, Void>() {
            @Override
            public Void map(TypeName arg) {
                if (!methodGenerics.contains(arg) && arg instanceof GenericName)
                    ((GenericName) arg).addQualifier(element.getSimpleName());
                return null;
            }
        });
        List<String> argNames = Utils.map(arguments, new Utils.MapFunction<VariableElement, String>() {
            @Override
            public String map(VariableElement arg) {
                return arg.toString();
            }
        });
        writer.addArgumentList(typeNames, argNames);
    }
    
    private List<TypeName> getThrownTypes(ExecutableElement exec, final List<TypeName> methodGenerics) {
        List<? extends TypeMirror> thrownTypeMirrors = exec.getThrownTypes();
        List<TypeName> thrownTypes = Utils.map(thrownTypeMirrors, new Utils.MapFunction<TypeMirror, TypeName>() {
            @Override
            public TypeName map(TypeMirror arg) {
                return Utils.getTypeNameFromTypeMirror(arg, null);
            }
        });
        Utils.map(thrownTypes, new Utils.MapFunction<TypeName, Void>() {
            @Override
            public Void map(TypeName arg) {
                if (!methodGenerics.contains(arg) && arg instanceof GenericName)
                    ((GenericName) arg).addQualifier(element.getSimpleName());
                return null;
            }
        });
        return thrownTypes;
    }

}
