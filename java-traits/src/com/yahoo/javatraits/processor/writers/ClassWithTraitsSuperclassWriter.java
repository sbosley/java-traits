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
import com.yahoo.aptutils.model.GenericName;
import com.yahoo.aptutils.model.MethodSignature;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.utils.Pair;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter.Type;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.aptutils.writer.parameters.TypeDeclarationParameters;
import com.yahoo.javatraits.processor.data.ClassWithTraits;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.processor.utils.TraitProcessorAptUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import java.io.IOException;
import java.util.*;

public class ClassWithTraitsSuperclassWriter extends JavaTraitsWriter<ClassWithTraits> {

    private List<TraitElement> allTraits;

    public ClassWithTraitsSuperclassWriter(ClassWithTraits cls, TraitProcessorAptUtils utils) {
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
            utils.accumulateImportsFromElements(imports, elem.getDeclaredMethods());
            imports.add(elem.getDelegateName());
            imports.add(elem.getGeneratedInterfaceName());
            if (elem.getConstants().size() > 0) {
                imports.add(elem.getElementName());
            }
        }
        DeclaredTypeName desiredSuperclass = element.getDesiredSuperclass();
        if (!AptUtils.OBJECT_CLASS_NAME.equals(desiredSuperclass.toString())) {
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
        final List<TypeName> generics = new ArrayList<TypeName>();
        final Map<String, Integer> knownGenericNames = new HashMap<String, Integer>();
        if (element.superclassHasTypeArgs()) {
            for (TypeName t : element.getDesiredSuperclass().getTypeArgs()) {
                if (t instanceof GenericName) {
                    generics.add(t);
                    knownGenericNames.put(((GenericName) t).getGenericName(), generics.size() - 1);
                }
            }
        }
        for (TraitElement elem : allTraits) {
            if (!AptUtils.isEmpty(elem.getTypeParameters())) {
                for (TypeName item : elem.getTypeParameters()) {
                    if (item instanceof GenericName) {
                        String genericName = ((GenericName) item).getGenericName();
                        if (knownGenericNames.containsKey(genericName)) {
                            generics.set(knownGenericNames.get(genericName), item);
                        } else {
                            generics.add(item);
                        }
                    } else {
                        generics.add(item);
                    }
                }
            }
        }
        DeclaredTypeName superclassName = element.getGeneratedSuperclassName().clone();
        superclassName.setTypeArgs(generics);

        List<DeclaredTypeName> interfaces = AptUtils.map(allTraits, new AptUtils.Function<TraitElement, DeclaredTypeName>() {
            @Override
            public DeclaredTypeName map(TraitElement arg) {
                return arg.getGeneratedInterfaceName();
            }
        });

        TypeDeclarationParameters params = new TypeDeclarationParameters()
            .setName(superclassName)
            .setKind(Type.CLASS)
            .setModifiers(Modifier.ABSTRACT)
            .setSuperclass(element.getDesiredSuperclass())
            .setInterfaces(interfaces);

        writer.beginTypeDefinition(params);

        emitConstants();
        emitDelegateFields();
        emitDelegateMethods();

        writer.finishTypeDefinition();
    }

    private void emitConstants() throws IOException {
        Set<String> constantNames = new HashSet<String>();
        Set<String> duplicateNames = new HashSet<String>();
        for (TraitElement elem : allTraits) {
            List<VariableElement> constants = elem.getConstants();
            for (VariableElement constant : constants) {
                String name = constant.getSimpleName().toString();
                if (!constantNames.add(name)) {
                    duplicateNames.add(name);
                }
            }
        }

        for (TraitElement elem : allTraits) {
            List<VariableElement> constants = elem.getConstants();
            for (VariableElement constant : constants) {
                TypeName constantType = utils.getTypeNameFromTypeMirror(constant.asType());
                String name = constant.getSimpleName().toString();
                if (duplicateNames.contains(name)) {
                    name = elem.getSimpleName() + "_" + name;
                }

                writer.writeFieldDeclaration(constantType, name, Expressions.staticReference(elem.getElementName(),
                        constant.getSimpleName().toString()), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
            }
        }
        if (constantNames.size() > 0) {
            writer.writeNewline();
        }
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
            emitMethodBody(elem, exec, methodDeclaration.getArgumentNames());
        }
    }
    
    private void emitMethodBody(TraitElement elem, ExecutableElement exec, List<?> arguments) throws IOException {
        String delegateVariableName = getDelegateVariableName(elem);
        
        Expression body = Expressions.callMethodOn(delegateVariableName, "default__" + exec.getSimpleName(), arguments);
        if (exec.getReturnType().getKind() != TypeKind.VOID) {
            body = body.returnExpr();
        }
        writer.writeStatement(body)
            .finishMethodDefinition();
    }
}
