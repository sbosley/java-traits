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
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter.Type;
import com.yahoo.aptutils.writer.expressions.Expression;
import com.yahoo.aptutils.writer.expressions.Expressions;
import com.yahoo.aptutils.writer.parameters.MethodDeclarationParameters;
import com.yahoo.aptutils.writer.parameters.TypeDeclarationParameters;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.processor.utils.TraitProcessorAptUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TraitDelegateWriter extends JavaTraitsWriter<TraitElement> {

    private DeclaredTypeName traitDelegateClass;
    private DeclaredTypeName delegateInterface;

    public TraitDelegateWriter(TraitElement traitElement, TraitProcessorAptUtils utils) {
        super(traitElement, utils);
        this.traitDelegateClass = traitElement.getDelegateName();
        this.delegateInterface = traitElement.getGeneratedInterfaceName();
    }

    @Override
    protected DeclaredTypeName getClassNameToGenerate() {
        return traitDelegateClass;
    }

    @Override
    protected void gatherImports(Set<DeclaredTypeName> imports) {
        utils.accumulateImportsFromElements(imports, element.getDeclaredMethods());
        for (int i = 0; i < element.getNumSuperinterfaces(); i++) {
            utils.accumulateImportsFromElements(imports, element.getExecutableElementsForInterface(i));
        }
    }

    protected void writeClassDefinition() throws IOException {
        DeclaredTypeName superclass = element.getElementName().clone();
        superclass.setTypeArgs(element.getTypeParameters());
        
        TypeDeclarationParameters params = new TypeDeclarationParameters()
            .setName(traitDelegateClass)
            .setKind(Type.CLASS)
            .setModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .setSuperclass(superclass);

        writer.registerOtherKnownNames(delegateInterface, element.getElementName());
        writer.beginTypeDefinition(params);

        emitDelegateInstance();
        emitConstructor();
        emitDefaultMethodImplementations();
        emitDelegateMethodImplementations();

        writer.finishTypeDefinition();
    }

    private void emitDelegateInstance() throws IOException {
        writer.writeFieldDeclaration(delegateInterface, "delegate", null, Modifier.PRIVATE);
    }

    private void emitConstructor() throws IOException {
        MethodDeclarationParameters params = new MethodDeclarationParameters()
            .setConstructorName(traitDelegateClass)
            .setModifiers(Modifier.PUBLIC)
            .setArgumentTypes(delegateInterface)
            .setArgumentNames("delegate");

        writer.beginConstructorDeclaration(params)
            .writeStringStatement("super()")
            .writeStatement(Expressions.assign(Expressions.reference("this", "delegate"), Expressions.reference("delegate")))
            .finishMethodDefinition();
    }

    private void emitDefaultMethodImplementations() throws IOException {
        List<? extends ExecutableElement> allMethods = element.getDeclaredMethods();
        for (ExecutableElement exec : allMethods) {
            if (!exec.getModifiers().contains(Modifier.ABSTRACT)) {
                emitMethodDeclaration(exec, null, true, Modifier.PUBLIC, Modifier.FINAL);
            }
        }
    }

    private void emitDelegateMethodImplementations() throws IOException {
        List<ExecutableElement> allMethods = element.getDeclaredMethods();
        for (ExecutableElement exec : allMethods) {
            if (utils.isGetThis(element, exec)) {
                emitGetThis();
            } else {
                emitMethodDeclaration(exec, null, false, Modifier.PUBLIC);
            }
        }

        emitInterfaceMethods();
    }

    private void emitGetThis() throws IOException {
        MethodDeclarationParameters params = new MethodDeclarationParameters()
            .setMethodName(TraitProcessorAptUtils.GET_THIS)
            .setReturnType(delegateInterface)
            .setModifiers(Modifier.PUBLIC);

        writer.beginMethodDefinition(params)
            .writeStatement(Expressions.reference("delegate").returnExpr())
            .finishMethodDefinition();
    }

    private void emitMethodDeclaration(ExecutableElement exec, Map<String, TypeName> genericNameMap, boolean isDefault, Modifier... modifiers) throws IOException {
        String name = isDefault ? "default__" + exec.getSimpleName().toString() : null;
        MethodDeclarationParameters methodDeclaration = utils.methodDeclarationParamsFromExecutableElement(exec, name, element.getSimpleName(), modifiers);
        remapMethodDeclarationGenerics(methodDeclaration, genericNameMap);
        writer.beginMethodDefinition(methodDeclaration);
        
        String callTo = isDefault ? "super" : "delegate";
        Expression methodInvocation = Expressions.callMethodOn(callTo, exec.getSimpleName().toString(), methodDeclaration.getArgumentNames());
        
        if (exec.getReturnType().getKind() != TypeKind.VOID) {
            methodInvocation = methodInvocation.returnExpr();
        }
        writer.writeStatement(methodInvocation)
            .finishMethodDefinition();
    }

    private void emitInterfaceMethods() throws IOException {
        for (int i = 0; i < element.getNumSuperinterfaces(); i++) {
            List<ExecutableElement> interfaceMethods = element.getExecutableElementsForInterface(i);
            for (ExecutableElement exec : interfaceMethods) {
                emitMethodDeclaration(exec, element.getGenericNameMapForInterface(i), false, Modifier.PUBLIC);
            }
        }
    }

    private void remapMethodDeclarationGenerics(MethodDeclarationParameters params, Map<String, TypeName> genericNameMap) {
        if (!AptUtils.isEmpty(genericNameMap)) {
            params.setReturnType(utils.remapGenericNames(params.getReturnType(), genericNameMap));
            params.setArgumentTypes(utils.remapGenericNames(params.getArgumentTypes(), genericNameMap));
            params.setThrowsTypes(utils.remapGenericNames(params.getThrowsTypes(), genericNameMap));
        }
    }

}
