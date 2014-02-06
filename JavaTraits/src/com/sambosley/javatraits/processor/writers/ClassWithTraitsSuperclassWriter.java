/**
 * Copyright 2014 Yahoo Inc.
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.sambosley.javatraits.processor.writers;

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
import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.sambosley.javatraits.annotations.HasTraits;
import com.sambosley.javatraits.processor.data.ClassWithTraits;
import com.sambosley.javatraits.processor.data.TraitElement;
import com.sambosley.javatraits.utils.ClassName;
import com.sambosley.javatraits.utils.JavaFileWriter;
import com.sambosley.javatraits.utils.MethodSignature;
import com.sambosley.javatraits.utils.Pair;
import com.sambosley.javatraits.utils.TypeName;
import com.sambosley.javatraits.utils.Utils;

public class ClassWithTraitsSuperclassWriter {

    private ClassWithTraits cls;
    private Map<ClassName, TraitElement> traitElementMap;
    private Messager messager;
    private List<TraitElement> allTraits;
    private JavaFileWriter writer;

    public ClassWithTraitsSuperclassWriter(ClassWithTraits cls, Map<ClassName, TraitElement> traitElementMap, Messager messager) {
        this.cls = cls;
        this.traitElementMap = traitElementMap;
        this.messager = messager;
        this.allTraits = Utils.map(Utils.getClassValuesFromAnnotation(HasTraits.class, cls.getSourceElement(), "traits", messager),
                new Utils.MapFunction<ClassName, TraitElement>() {
            public TraitElement map(ClassName fqn) {
                return ClassWithTraitsSuperclassWriter.this.traitElementMap.get(fqn);
            };
        });;
    }

    public void writeClass(Filer filer) {
        try {
            if (writer != null)
                throw new IllegalStateException("Already created source file for " + cls.getFullyQualifiedGeneratedSuperclassName().toString());
            JavaFileObject jfo = filer.createSourceFile(cls.getFullyQualifiedGeneratedSuperclassName().toString(),
                    cls.getSourceElement());
            Writer out = jfo.openWriter();
            writer = new JavaFileWriter(out);
            emitClassDefinition();
            writer.close();
        } catch (IOException e) {
            messager.printMessage(Kind.ERROR, "IOException writing delegate class with delegate " + 
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
            Utils.accumulateImportsFromExecutableElements(imports, declaredMethods, messager);
            imports.add(cls.getDelegateClassNameForTraitElement(elem));
            imports.add(elem.getInterfaceName());
        }
        ClassName desiredSuperclass = cls.getDesiredSuperclass();
        if (!Utils.OBJECT_CLASS_NAME.equals(desiredSuperclass.toString()))
            imports.add(desiredSuperclass);

        writer.writeImports(imports);
    }

    private void emitClassDeclaration() throws IOException {
        writer.beginTypeDeclaration(cls.getFullyQualifiedGeneratedSuperclassName().getSimpleName(), "class", Modifier.PUBLIC, Modifier.ABSTRACT);
        List<TypeName> generics = new ArrayList<TypeName>();
        for (int i = 0; i < allTraits.size(); i++) {
            TraitElement elem = allTraits.get(i);
            if (elem.hasTypeParameters()) {
                generics.addAll(elem.getTypeParameters());
            }
        }
        writer.appendGenericDeclaration(generics);

        String desiredSuperclass = cls.getDesiredSuperclass().toString();
        if (!Utils.OBJECT_CLASS_NAME.equals(desiredSuperclass))
            writer.addSuperclassToTypeDeclaration(cls.getDesiredSuperclass(), null);
        
        if (allTraits.size() > 0) {
            List<ClassName> interfaces = new ArrayList<ClassName>();
            List<List<? extends TypeName>> interfaceGenerics = new ArrayList<List<? extends TypeName>>();
            
            for (int i = 0; i < allTraits.size(); i++) {
                TraitElement elem = allTraits.get(i);
                interfaces.add(elem.getInterfaceName());
                interfaceGenerics.add(elem.getTypeParameters());
            }
            writer.addInterfacesToTypeDeclaration(interfaces, interfaceGenerics);
        }

        writer.finishTypeDeclarationAndBeginTypeDefinition();

        emitDelegateFields();
        emitInitMethod();
        emitDelegateMethods();

        writer.finishTypeDefinitionAndCloseType();
    }

    private void emitDelegateFields() throws IOException {
        for (TraitElement elem : allTraits) {
            ClassName delegateClass = cls.getDelegateClassNameForTraitElement(elem);
            writer.emitFieldDeclaration(delegateClass, getDelegateVariableName(elem), elem.getTypeParameters(), Modifier.PRIVATE);
        }
        writer.emitNewline();
    }

    private void emitInitMethod() throws IOException {
        writer.beginMethodDeclaration("init", null, Arrays.asList(Modifier.PROTECTED, Modifier.FINAL), null);
        writer.finishMethodDeclarationAndBeginMethodDefinition(null, false);
        
        for (TraitElement elem : allTraits) {
            ClassName delegateClass = cls.getDelegateClassNameForTraitElement(elem);
            writer.emitStatement(getDelegateVariableName(elem) + " = new ", 2);
            writer.emitStatement(writer.shortenName(delegateClass), 0);
            writer.emitGenericsList(elem.getTypeParameters(), false);
            writer.emitStatement("(this);\n", 0);
        }
        writer.finishMethodDefinition();
    }

    private String getDelegateVariableName(TraitElement elem) {
        String base = elem.getSimpleName();
        return base.substring(0, 1).toLowerCase() + base.substring(1) + ClassWithTraits.DELEGATE_SUFFIX;
    }

    private void emitDelegateMethods() throws IOException {
        Set<MethodSignature> dupes = new HashSet<MethodSignature>();
        Map<MethodSignature, List<Pair<TraitElement, ExecutableElement>>> methodToExecElements = new HashMap<MethodSignature, List<Pair<TraitElement, ExecutableElement>>>();
        for (TraitElement elem : allTraits) {
            List<? extends ExecutableElement> execElems = elem.getDeclaredMethods();
            for (ExecutableElement exec : execElems) {
                MethodSignature signature = Utils.getMethodSignature(exec, elem.getSimpleName());
                List<Pair<TraitElement, ExecutableElement>> elements = methodToExecElements.get(signature);
                if (elements == null) {
                    elements = new ArrayList<Pair<TraitElement, ExecutableElement>>();
                    methodToExecElements.put(signature, elements);
                } else {
                    dupes.add(signature);
                }
                elements.add(Pair.create(elem, exec));
            }
        }

        if (!dupes.isEmpty()) {
            Map<String, ClassName> prefer = cls.getPreferMap();
            for (MethodSignature dup : dupes) {
                String simpleMethodName = dup.getMethodName();
                if (prefer.containsKey(simpleMethodName)) {
                    ClassName preferTarget = prefer.get(simpleMethodName);
                    List<Pair<TraitElement, ExecutableElement>> allExecElems = methodToExecElements.get(dup);
                    int index = 0;
                    for (index = 0; index < allExecElems.size(); index++) {
                        Pair<TraitElement, ExecutableElement> item = allExecElems.get(index);
                        if (item.getLeft().getFullyQualifiedName().equals(preferTarget))
                            break;
                    }
                    if (index > 0) {
                        Pair<TraitElement, ExecutableElement> item = allExecElems.remove(index);
                        allExecElems.add(0, item);
                    }
                }
            }
        }

        for (List<Pair<TraitElement, ExecutableElement>> executablePairList : methodToExecElements.values()) {
            Pair<TraitElement, ExecutableElement> executablePair = executablePairList.get(0);
            TraitElement elem = executablePair.getLeft();
            ExecutableElement exec = executablePair.getRight();

            Set<Modifier> modifiers = exec.getModifiers();
            boolean isAbstract = modifiers.contains(Modifier.ABSTRACT);
            List<String> argNames = Utils.beginMethodDeclarationForExecutableElement(writer, exec, null, elem.getSimpleName(), isAbstract, modifiers.toArray(new Modifier[modifiers.size()]));
            
            if (!isAbstract) {
                StringBuilder nullCheck = new StringBuilder();
                String delegateVariableName = getDelegateVariableName(elem);
                nullCheck.append("if (").append(delegateVariableName).append(" == null)");
                writer.emitStatement("if (" + delegateVariableName + " == null)\n", 2);
                writer.emitStatement("throw new IllegalStateException(\"init() not called on instance of class \" + getClass());\n", 3);
                
                StringBuilder statement = new StringBuilder();
                if (exec.getReturnType().getKind() != TypeKind.VOID)
                    statement.append("return ");
                statement.append(delegateVariableName)
                .append(".").append("default__").append(exec.getSimpleName()).append("(");
                for (int i = 0; i < argNames.size(); i++) {
                    statement.append(argNames.get(i));
                    if (i < argNames.size() - 1)
                        statement.append(", ");
                }
                statement.append(");\n");
                writer.emitStatement(statement.toString(), 2);
                writer.finishMethodDefinition();
            }
        }
    }
}
