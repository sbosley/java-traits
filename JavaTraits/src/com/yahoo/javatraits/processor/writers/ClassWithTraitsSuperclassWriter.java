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

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.model.MethodSignature;
import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.utils.Pair;
import com.yahoo.annotations.utils.Utils;
import com.yahoo.annotations.writer.JavaFileWriter;
import com.yahoo.annotations.writer.JavaFileWriter.Type;
import com.yahoo.annotations.writer.expressions.ConstructorInvocation;
import com.yahoo.annotations.writer.expressions.Expression;
import com.yahoo.annotations.writer.expressions.MethodInvocation;
import com.yahoo.annotations.writer.expressions.ReturnExpression;
import com.yahoo.annotations.writer.parameters.MethodDeclarationParameters;
import com.yahoo.annotations.writer.parameters.TypeDeclarationParameters;
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
        Set<DeclaredTypeName> imports = new HashSet<DeclaredTypeName>();
        for (TraitElement elem : allTraits) {
            List<? extends ExecutableElement> declaredMethods = elem.getDeclaredMethods();
            utils.accumulateImportsFromExecutableElements(imports, declaredMethods);
            imports.add(elem.getDelegateName());
            imports.add(elem.getInterfaceName());
        }
        DeclaredTypeName desiredSuperclass = cls.getDesiredSuperclass();
        if (!Utils.OBJECT_CLASS_NAME.equals(desiredSuperclass.toString())) {
            imports.add(desiredSuperclass);
            if (cls.superclassHasTypeArgs()) {
                List<? extends TypeName> superclassTypeArgs = cls.getDesiredSuperclass().getTypeArgs();
                for (TypeName t : superclassTypeArgs) {
                    if (t instanceof DeclaredTypeName) {
                        imports.add((DeclaredTypeName) t);
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
                if (!(t instanceof DeclaredTypeName)) {
                    generics.add(t);
                }
            }
        }
        for (TraitElement elem : allTraits) {
            if (elem.hasTypeParameters()) {
                generics.addAll(elem.getTypeParameters());
            }
        }
        DeclaredTypeName superclassName = cls.getFullyQualifiedGeneratedSuperclassName().clone();
        superclassName.setTypeArgs(generics);

        List<DeclaredTypeName> interfaces = null;
        if (allTraits.size() > 0) {
            interfaces = new ArrayList<DeclaredTypeName>();
            for (int i = 0; i < allTraits.size(); i++) {
                TraitElement elem = allTraits.get(i);
                interfaces.add(elem.getInterfaceName());
            }
        }

        TypeDeclarationParameters params = new TypeDeclarationParameters()
            .setName(superclassName)
            .setKind(Type.CLASS)
            .setModifiers(Arrays.asList(Modifier.PUBLIC, Modifier.ABSTRACT))
            .setSuperclass(cls.getDesiredSuperclass())
            .setInterfaces(interfaces);

        writer.beginTypeDefinition(params);

        emitDelegateFields();
        emitDelegateMethods();

        writer.finishTypeDefinition();
    }

    private void emitDelegateFields() throws IOException {
        for (TraitElement elem : allTraits) {
            DeclaredTypeName delegateClass = elem.getDelegateName();
            ConstructorInvocation init = new ConstructorInvocation(delegateClass, Arrays.asList("this"));
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
                MethodSignature signature = utils.executableElementToMethodSignature(exec, elem.getSimpleName());
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
        
        Map<String, DeclaredTypeName> prefer = cls.getPreferMap();
        for (MethodSignature dup : duplicateMethods) {
            String simpleMethodName = dup.getMethodName();
            if (prefer.containsKey(simpleMethodName)) {
                DeclaredTypeName preferTarget = prefer.get(simpleMethodName);
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
        MethodDeclarationParameters methodDeclaration = utils.methodDeclarationParamsFromExecutableElement(exec, null, elem.getSimpleName(), modifiers.toArray(new Modifier[modifiers.size()]));
        writer.beginMethodDefinition(methodDeclaration);
        
        if (!isAbstract) {
            emitMethodBody(elem, exec, methodDeclaration.getArgumentNames());
        }
    }
    
    private void emitMethodBody(TraitElement elem, ExecutableElement exec, List<String> argNames) throws IOException {
        String delegateVariableName = getDelegateVariableName(elem);
        
        Expression body = new MethodInvocation(delegateVariableName, "default__" + exec.getSimpleName(), argNames);
        if (exec.getReturnType().getKind() != TypeKind.VOID) {
            body = new ReturnExpression(body);
        }
        writer.writeStatement(body);
        writer.finishMethodDefinition();
    }
}
