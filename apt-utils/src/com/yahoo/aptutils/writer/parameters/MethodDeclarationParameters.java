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
package com.yahoo.aptutils.writer.parameters;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.utils.AptUtils;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * An object for containing the information needed to begin a method declaration. Required by
 * {@link com.yahoo.aptutils.writer.JavaFileWriter#beginMethodDefinition(MethodDeclarationParameters)}
 *
 * An instance of this class can be constructed by calling the no-arg constructor and then chaining method calls:
 *
 * new MethodDeclarationParameters().setMethodName("methodName").setReturnType(returnType) etc.
 */
public class MethodDeclarationParameters {

    private DeclaredTypeName constructorName;
    private String methodName;
    private TypeName returnType;
    private List<Modifier> modifiers;
    private List<? extends TypeName> methodGenerics;
    private List<? extends TypeName> argumentTypes;
    private List<String> argumentNames;
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
        this.modifiers = AptUtils.asList(modifiers);
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
        this.argumentTypes = AptUtils.asList(argumentTypes);
        return this;
    }
    
    public MethodDeclarationParameters setArgumentTypes(List<? extends TypeName> argumentTypes) {
        this.argumentTypes = argumentTypes;
        return this;
    }
    
    public List<String> getArgumentNames() {
        return argumentNames;
    }
    
    public MethodDeclarationParameters setArgumentNames(String... arguments) {
        this.argumentNames = AptUtils.asList(arguments);
        return this;
    }
    
    public MethodDeclarationParameters setArgumentNames(List<String> arguments) {
        this.argumentNames = arguments;
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
