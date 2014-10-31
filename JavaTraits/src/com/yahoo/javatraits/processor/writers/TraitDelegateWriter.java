/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.writers;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.writer.JavaFileWriter.Type;
import com.yahoo.annotations.writer.expressions.Expression;
import com.yahoo.annotations.writer.expressions.Expressions;
import com.yahoo.annotations.writer.parameters.MethodDeclarationParameters;
import com.yahoo.annotations.writer.parameters.TypeDeclarationParameters;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.processor.utils.TraitProcessorUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class TraitDelegateWriter extends JavaTraitsWriter<TraitElement> {

    private DeclaredTypeName traitDelegateClass;
    private DeclaredTypeName delegateClass;

    public TraitDelegateWriter(TraitElement traitElement, TraitProcessorUtils utils) {
        super(traitElement, utils);
        this.traitDelegateClass = traitElement.getDelegateName();
        this.delegateClass = traitElement.getInterfaceName();
    }

    @Override
    protected DeclaredTypeName getClassNameToGenerate() {
        return traitDelegateClass;
    }

    @Override
    protected void gatherImports(Set<DeclaredTypeName> imports) {
        utils.accumulateImportsFromElements(imports, element.getDeclaredMethods());
        for (ExecutableElement e : element.getDeclaredMethods()) {
            if (utils.isGetThis(element, e)) {
                imports.add(element.getInterfaceName());
            }
        }
        imports.add(delegateClass);
        imports.add(element.getElementName());
    }

    protected void writeClassDefinition() throws IOException {
        DeclaredTypeName superclass = element.getElementName().clone();
        superclass.setTypeArgs(element.getTypeParameters());
        
        TypeDeclarationParameters params = new TypeDeclarationParameters()
            .setName(traitDelegateClass)
            .setKind(Type.CLASS)
            .setModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .setSuperclass(superclass);

        writer.beginTypeDefinition(params);

        emitDelegateInstance();
        emitConstructor();
        emitDefaultMethodImplementations();
        emitDelegateMethodImplementations();

        writer.finishTypeDefinition();
    }

    private void emitDelegateInstance() throws IOException {
        writer.writeFieldDeclaration(delegateClass, "delegate", null, Modifier.PRIVATE);
    }

    private void emitConstructor() throws IOException {
        MethodDeclarationParameters params = new MethodDeclarationParameters()
            .setConstructorName(traitDelegateClass)
            .setModifiers(Modifier.PUBLIC)
            .setArgumentTypes(delegateClass)
            .setArgumentNames("delegate");

        writer.beginConstructorDeclaration(params);
        writer.writeStatement(Expressions.assign(Expressions.reference("this", "delegate"), Expressions.reference("delegate")));
        writer.finishMethodDefinition();
    }

    private void emitDefaultMethodImplementations() throws IOException {
        List<? extends ExecutableElement> allMethods = element.getDeclaredMethods();
        for (ExecutableElement exec : allMethods) {
            if (!exec.getModifiers().contains(Modifier.ABSTRACT)) {
                emitMethodDeclaration(exec, true, Modifier.PUBLIC, Modifier.FINAL);
            }
        }
    }

    private void emitDelegateMethodImplementations() throws IOException {
        List<? extends ExecutableElement> allMethods = element.getDeclaredMethods();
        for (ExecutableElement exec : allMethods) {
            if (utils.isGetThis(element, exec)) {
                emitGetThis();
            } else {
                emitMethodDeclaration(exec, false, Modifier.PUBLIC);
            }
        }
    }

    private void emitGetThis() throws IOException {
        MethodDeclarationParameters params = new MethodDeclarationParameters()
            .setMethodName(TraitProcessorUtils.GET_THIS)
            .setReturnType(delegateClass)
            .setModifiers(Modifier.PUBLIC);

        writer.beginMethodDefinition(params);
        writer.writeStatement(Expressions.reference("delegate").returnExpr());
        writer.finishMethodDefinition();
    }

    private void emitMethodDeclaration(ExecutableElement exec, boolean isDefault, Modifier... modifiers) throws IOException {
        String name = isDefault ? "default__" + exec.getSimpleName().toString() : null;
        MethodDeclarationParameters methodDeclaration = utils.methodDeclarationParamsFromExecutableElement(exec, name, element.getSimpleName(), modifiers);
        writer.beginMethodDefinition(methodDeclaration);
        
        String callTo = isDefault ? "super" : "delegate";
        Expression methodInvocation = Expressions.callMethodOn(callTo, exec.getSimpleName().toString(), methodDeclaration.getArguments());
        
        if (exec.getReturnType().getKind() != TypeKind.VOID) {
            methodInvocation = methodInvocation.returnExpr();
        }
        writer.writeStatement(methodInvocation);
        writer.finishMethodDefinition();
    }

}
