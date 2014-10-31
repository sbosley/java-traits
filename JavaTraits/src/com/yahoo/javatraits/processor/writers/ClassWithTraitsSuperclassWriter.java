/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.writers;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.model.MethodSignature;
import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.utils.Pair;
import com.yahoo.annotations.utils.Utils;
import com.yahoo.annotations.writer.JavaFileWriter.Type;
import com.yahoo.annotations.writer.expressions.Expression;
import com.yahoo.annotations.writer.expressions.Expressions;
import com.yahoo.annotations.writer.parameters.MethodDeclarationParameters;
import com.yahoo.annotations.writer.parameters.TypeDeclarationParameters;
import com.yahoo.javatraits.processor.data.ClassWithTraits;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.processor.utils.TraitProcessorUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import java.io.IOException;
import java.util.*;

public class ClassWithTraitsSuperclassWriter extends JavaTraitsWriter<ClassWithTraits> {

    private List<TraitElement> allTraits;

    public ClassWithTraitsSuperclassWriter(ClassWithTraits cls, TraitProcessorUtils utils) {
        super(cls, utils);
        this.allTraits = cls.getTraitClasses();
    }

    @Override
    protected DeclaredTypeName getClassNameToGenerate() {
        return element.getGeneratedSuperclassName();
    }

    @Override
    protected void gatherImports(Set<DeclaredTypeName> imports) {
        for (TraitElement elem : allTraits) {
            List<? extends ExecutableElement> declaredMethods = elem.getDeclaredMethods();
            utils.accumulateImportsFromElements(imports, declaredMethods);
            imports.add(elem.getDelegateName());
            imports.add(elem.getInterfaceName());
        }
        DeclaredTypeName desiredSuperclass = element.getDesiredSuperclass();
        if (!Utils.OBJECT_CLASS_NAME.equals(desiredSuperclass.toString())) {
            imports.add(desiredSuperclass);
            if (element.superclassHasTypeArgs()) {
                List<? extends TypeName> superclassTypeArgs = element.getDesiredSuperclass().getTypeArgs();
                for (TypeName t : superclassTypeArgs) {
                    if (t instanceof DeclaredTypeName) {
                        imports.add((DeclaredTypeName) t);
                    }
                }
            }
        }
    }

    protected void writeClassDefinition() throws IOException {
        List<TypeName> generics = new ArrayList<TypeName>();
        if (element.superclassHasTypeArgs()) {
            for (TypeName t : element.getDesiredSuperclass().getTypeArgs()) {
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
        DeclaredTypeName superclassName = element.getGeneratedSuperclassName().clone();
        superclassName.setTypeArgs(generics);

        List<DeclaredTypeName> interfaces = null;
        if (allTraits.size() > 0) {
            interfaces = new ArrayList<DeclaredTypeName>();
            for (TraitElement elem : allTraits) {
                interfaces.add(elem.getInterfaceName());
            }
        }

        TypeDeclarationParameters params = new TypeDeclarationParameters()
            .setName(superclassName)
            .setKind(Type.CLASS)
            .setModifiers(Modifier.ABSTRACT)
            .setSuperclass(element.getDesiredSuperclass())
            .setInterfaces(interfaces);

        writer.beginTypeDefinition(params);

        emitDelegateFields();
        emitDelegateMethods();

        writer.finishTypeDefinition();
    }

    private void emitDelegateFields() throws IOException {
        for (TraitElement elem : allTraits) {
            DeclaredTypeName delegateClass = elem.getDelegateName();
            Expression init = Expressions.callConstructor(delegateClass, "this");
            writer.writeFieldDeclaration(delegateClass, getDelegateVariableName(elem), init, Modifier.PRIVATE);
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
        
        Map<String, DeclaredTypeName> prefer = element.getPreferMap();
        for (MethodSignature dup : duplicateMethods) {
            String simpleMethodName = dup.getMethodName();
            if (prefer.containsKey(simpleMethodName)) {
                DeclaredTypeName preferTarget = prefer.get(simpleMethodName);
                List<Pair<TraitElement, ExecutableElement>> allExecElems = methodToExecElements.get(dup);
                int index;
                for (index = 0; index < allExecElems.size(); index++) {
                    Pair<TraitElement, ExecutableElement> item = allExecElems.get(index);
                    if (item.getLeft().getElementName().equals(preferTarget)) {
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
            emitMethodBody(elem, exec, methodDeclaration.getArguments());
        }
    }
    
    private void emitMethodBody(TraitElement elem, ExecutableElement exec, List<?> arguments) throws IOException {
        String delegateVariableName = getDelegateVariableName(elem);
        
        Expression body = Expressions.callMethodOn(delegateVariableName, "default__" + exec.getSimpleName(), arguments);
        if (exec.getReturnType().getKind() != TypeKind.VOID) {
            body = body.returnExpr();
        }
        writer.writeStatement(body);
        writer.finishMethodDefinition();
    }
}
