package com.yahoo.annotations.writer.parameters;

import java.util.List;

import javax.lang.model.element.Modifier;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.utils.Utils;

public class MethodDeclarationParameters {

    private DeclaredTypeName constructorName;
    private String methodName;
    private TypeName returnType;
    private List<Modifier> modifiers;
    private List<? extends TypeName> methodGenerics;
    private List<? extends TypeName> argumentTypes;
    private List<?> arguments;
    private List<? extends TypeName> throwsTypes;
    
    public boolean isConstructor() {
        return getConstructorName() != null;
    }
    
    public DeclaredTypeName getConstructorName() {
        return constructorName;
    }
    
    public MethodDeclarationParameters setConstructorName(DeclaredTypeName constructorName) {
        this.constructorName = constructorName;
        return this;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public MethodDeclarationParameters setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }
    
    public TypeName getReturnType() {
        return returnType;
    }
    
    public MethodDeclarationParameters setReturnType(TypeName returnType) {
        this.returnType = returnType;
        return this;
    }
    
    public List<Modifier> getModifiers() {
        return modifiers;
    }
    
    public MethodDeclarationParameters setModifiers(Modifier... modifiers) {
        this.modifiers = Utils.asList(modifiers);
        return this;
    }
    
    public MethodDeclarationParameters setModifiers(List<Modifier> modifiers) {
        this.modifiers = modifiers;
        return this;
    }
    
    public List<? extends TypeName> getMethodGenerics() {
        return methodGenerics;
    }
    
    public MethodDeclarationParameters setMethodGenerics(List<? extends TypeName> methodGenerics) {
        this.methodGenerics = methodGenerics;
        return this;
    }
    
    public List<? extends TypeName> getArgumentTypes() {
        return argumentTypes;
    }
    
    public MethodDeclarationParameters setArgumentTypes(TypeName... argumentTypes) {
        this.argumentTypes = Utils.asList(argumentTypes);
        return this;
    }
    
    public MethodDeclarationParameters setArgumentTypes(List<? extends TypeName> argumentTypes) {
        this.argumentTypes = argumentTypes;
        return this;
    }
    
    public List<?> getArguments() {
        return arguments;
    }
    
    public MethodDeclarationParameters setArgumentNames(String... arguments) {
        this.arguments = Utils.asList(arguments);
        return this;
    }
    
    public MethodDeclarationParameters setArgumentNames(List<String> arguments) {
        this.arguments = arguments;
        return this;
    }
    
    public List<? extends TypeName> getThrowsTypes() {
        return throwsTypes;
    }
    
    public MethodDeclarationParameters setThrowsTypes(List<? extends TypeName> throwsTypes) {
        this.throwsTypes = throwsTypes;
        return this;
    }
    
}
