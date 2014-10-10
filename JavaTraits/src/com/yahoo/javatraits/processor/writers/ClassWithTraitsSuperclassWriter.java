/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.writers;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.yahoo.annotations.model.ClassName;
import com.yahoo.annotations.model.MethodSignature;
import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.utils.Pair;
import com.yahoo.annotations.utils.Utils;
import com.yahoo.annotations.writer.JavaFileWriter;
import com.yahoo.annotations.writer.JavaFileWriter.ConstructorInitialization;
import com.yahoo.annotations.writer.JavaFileWriter.Type;
import com.yahoo.annotations.writer.JavaFileWriter.TypeDeclarationParameters;
import com.yahoo.javatraits.processor.data.ClassWithTraits;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.processor.utils.TraitProcessorUtils;

public class ClassWithTraitsSuperclassWriter {

    private ClassWithTraits cls;
    private TraitProcessorUtils utils;
    private List<TraitElement> allTraits;
    private JavaFileWriter writer;

    public ClassWithTraitsSuperclassWriter(ClassWithTraits cls, TraitProcessorUtils utils) {
        this.cls = cls;
        this.utils = utils;
        this.allTraits = cls.getTraitClasses();
    }

    public void writeClass(Filer filer) {
        try {
            if (writer != null) {
                throw new IllegalStateException("Already created source file for " + cls.getFullyQualifiedGeneratedSuperclassName().toString());
            }
            JavaFileObject jfo = filer.createSourceFile(cls.getFullyQualifiedGeneratedSuperclassName().toString(),
                    cls.getSourceElement());
            Writer out = jfo.openWriter();
            writer = new JavaFileWriter(out);
            emitClassDefinition();
            writer.close();
        } catch (IOException e) {
            utils.getMessager().printMessage(Kind.ERROR, "IOException writing delegate class with delegate " +
                    cls.getSimpleName() + " for trait", cls.getSourceElement());
        }
    }

    private void emitClassDefinition() throws IOException {
        emitPackage();
        emitImports();
        emitClassDeclaration();
    }

    private void emitPackage() throws IOException {
        writer.writePackage(cls.getPackageName());
    }

    private void emitImports() throws IOException {
        Set<ClassName> imports = new HashSet<ClassName>();
        for (TraitElement elem : allTraits) {
            List<? extends ExecutableElement> declaredMethods = elem.getDeclaredMethods();
            utils.accumulateImportsFromExecutableElements(imports, declaredMethods);
            imports.add(elem.getDelegateName());
            imports.add(elem.getInterfaceName());
        }
        ClassName desiredSuperclass = cls.getDesiredSuperclass();
        if (!Utils.OBJECT_CLASS_NAME.equals(desiredSuperclass.toString())) {
            imports.add(desiredSuperclass);
            if (cls.superclassHasTypeArgs()) {
                List<? extends TypeName> superclassTypeArgs = cls.getDesiredSuperclass().getTypeArgs();
                for (TypeName t : superclassTypeArgs) {
                    if (t instanceof ClassName) {
                        imports.add((ClassName) t);
                    }
                }
            }
        }

        writer.writeImports(imports);
    }

    private void emitClassDeclaration() throws IOException {
        List<TypeName> generics = new ArrayList<TypeName>();
        if (cls.superclassHasTypeArgs()) {
            for (TypeName t : cls.getDesiredSuperclass().getTypeArgs()) {
                if (!(t instanceof ClassName)) {
                    generics.add(t);
                }
            }
        }
        for (TraitElement elem : allTraits) {
            if (elem.hasTypeParameters()) {
                generics.addAll(elem.getTypeParameters());
            }
        }
        ClassName superclassName = cls.getFullyQualifiedGeneratedSuperclassName().clone();
        superclassName.setTypeArgs(generics);

        List<ClassName> interfaces = null;
        if (allTraits.size() > 0) {
            interfaces = new ArrayList<ClassName>();
            for (int i = 0; i < allTraits.size(); i++) {
                TraitElement elem = allTraits.get(i);
                interfaces.add(elem.getInterfaceName());
            }
        }

        TypeDeclarationParameters params = new TypeDeclarationParameters();
        params.name = superclassName;
        params.kind = Type.CLASS;
        params.modifiers = Arrays.asList(Modifier.PUBLIC, Modifier.ABSTRACT);
        params.superclass = cls.getDesiredSuperclass();
        params.interfaces = interfaces;

        writer.beginTypeDefinition(params);

        emitDelegateFields();
        emitDelegateMethods();

        writer.finishTypeDefinition();
    }

    private void emitDelegateFields() throws IOException {
        for (TraitElement elem : allTraits) {
            ClassName delegateClass = elem.getDelegateName();
            ConstructorInitialization init = new ConstructorInitialization();
            init.constructorType = delegateClass;
            init.argumentNames = Arrays.asList("this");
            writer.writeFieldDeclaration(delegateClass, getDelegateVariableName(elem), Arrays.asList(Modifier.PRIVATE), init);
        }
        writer.writeNewline();
    }

    private String getDelegateVariableName(TraitElement elem) {
        String base = elem.getDelegateName().getSimpleName();
        return base.substring(0, 1).toLowerCase() + base.substring(1);
    }

    private void emitDelegateMethods() throws IOException {
        Set<MethodSignature> duplicateMethods = new HashSet<MethodSignature>();
        Map<MethodSignature, List<Pair<TraitElement, ExecutableElement>>> methodToExecElements = new HashMap<MethodSignature, List<Pair<TraitElement, ExecutableElement>>>();
        
        accumulateMethods(duplicateMethods, methodToExecElements);

        if (!duplicateMethods.isEmpty()) {
            reorderDuplicatesForPreferValues(duplicateMethods, methodToExecElements);
        }

        for (List<Pair<TraitElement, ExecutableElement>> executablePairList : methodToExecElements.values()) {
            Pair<TraitElement, ExecutableElement> executablePair = executablePairList.get(0);
            emitMethodDefinition(executablePair.getLeft(), executablePair.getRight());
        }
    }

    private void accumulateMethods(Set<MethodSignature> duplicateMethods, 
            Map<MethodSignature, List<Pair<TraitElement, ExecutableElement>>> methodToExecElements) {
        
        for (TraitElement elem : allTraits) {
            List<? extends ExecutableElement> execElems = elem.getDeclaredMethods();
            for (ExecutableElement exec : execElems) {
                MethodSignature signature = utils.getMethodSignature(exec, elem.getSimpleName());
                List<Pair<TraitElement, ExecutableElement>> elements = methodToExecElements.get(signature);
                if (elements == null) {
                    elements = new ArrayList<Pair<TraitElement, ExecutableElement>>();
                    methodToExecElements.put(signature, elements);
                } else {
                    duplicateMethods.add(signature);
                }
                elements.add(Pair.create(elem, exec));
            }
        }
    }
    
    private void reorderDuplicatesForPreferValues(Set<MethodSignature> duplicateMethods,
            Map<MethodSignature, List<Pair<TraitElement, ExecutableElement>>> methodToExecElements) {
        
        Map<String, ClassName> prefer = cls.getPreferMap();
        for (MethodSignature dup : duplicateMethods) {
            String simpleMethodName = dup.getMethodName();
            if (prefer.containsKey(simpleMethodName)) {
                ClassName preferTarget = prefer.get(simpleMethodName);
                List<Pair<TraitElement, ExecutableElement>> allExecElems = methodToExecElements.get(dup);
                int index = 0;
                for (index = 0; index < allExecElems.size(); index++) {
                    Pair<TraitElement, ExecutableElement> item = allExecElems.get(index);
                    if (item.getLeft().getFullyQualifiedName().equals(preferTarget)) {
                        break;
                    }
                }
                if (index > 0) {
                    Pair<TraitElement, ExecutableElement> item = allExecElems.remove(index);
                    allExecElems.add(0, item);
                }
            }
        }
    }
    
    private void emitMethodDefinition(TraitElement elem, ExecutableElement exec) throws IOException {
        if (utils.isGetThis(elem, exec)) {
            return;
        }

        Set<Modifier> modifiers = exec.getModifiers();
        boolean isAbstract = modifiers.contains(Modifier.ABSTRACT);
        List<String> argNames = utils.beginMethodDeclarationForExecutableElement(writer, exec, null, elem.getSimpleName(), modifiers.toArray(new Modifier[modifiers.size()]));

        if (!isAbstract) {
            emitMethodBody(elem, exec, argNames);
        }
    }
    
    private void emitMethodBody(TraitElement elem, ExecutableElement exec, List<String> argNames) throws IOException {
        String delegateVariableName = getDelegateVariableName(elem);
        StringBuilder statement = new StringBuilder();
        if (exec.getReturnType().getKind() != TypeKind.VOID) {
            statement.append("return ");
        }
        statement.append(delegateVariableName)
        .append(".").append("default__").append(exec.getSimpleName()).append("(");
        for (int i = 0; i < argNames.size(); i++) {
            statement.append(argNames.get(i));
            if (i < argNames.size() - 1) {
                statement.append(", ");
            }
        }
        statement.append(");\n");
        writer.writeStatement(statement.toString());
        writer.finishMethodDefinition();
    }
}
