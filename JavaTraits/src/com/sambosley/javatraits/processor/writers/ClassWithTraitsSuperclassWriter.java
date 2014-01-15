/**
 * Copyright 2014 Sam Bosley
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.sambosley.javatraits.processor.writers;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
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
import com.sambosley.javatraits.utils.Pair;
import com.sambosley.javatraits.utils.Utils;

public class ClassWithTraitsSuperclassWriter {

    private ClassWithTraits cls;
    private Map<ClassName, TraitElement> traitElementMap;
    private Messager messager;
    private List<TraitElement> allTraits;

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
            JavaFileObject jfo = filer.createSourceFile(cls.getFullyQualifiedGeneratedSuperclassName().toString(),
                    cls.getSourceElement());
            Writer writer = jfo.openWriter();
            writer.write(emitClassDefinition());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            messager.printMessage(Kind.ERROR, "IOException writing delegate class with delegate " + 
                    cls.getSimpleName() + " for trait", cls.getSourceElement());
        }
    }

    private String emitClassDefinition() {
        StringBuilder builder = new StringBuilder();
        emitPackage(builder);
        emitImports(builder);
        emitClassDeclaration(builder);
        return builder.toString();
    }

    private void emitPackage(StringBuilder builder) {
        builder.append("package ").append(cls.getPackageName()).append(";\n\n");
    }

    private void emitImports(StringBuilder builder) {
        Set<String> imports = new HashSet<String>();
        for (TraitElement elem : allTraits) {
            List<? extends ExecutableElement> declaredMethods = elem.getDeclaredMethods();
            Utils.accumulateImportsFromExecutableElements(imports, declaredMethods, messager);
            imports.add(cls.getDelegateClassNameForTraitElement(elem).toString());
        }
        String desiredSuperclass = cls.getDesiredSuperclass().toString();
        if (!Utils.OBJECT_CLASS_NAME.equals(desiredSuperclass))
            imports.add(desiredSuperclass);

        for (String s : imports) {
            builder.append("import ").append(s).append(";\n");
        }
        builder.append("\n");
    }

    private void emitClassDeclaration(StringBuilder builder) {
        builder.append("public abstract class ").append(cls.getFullyQualifiedGeneratedSuperclassName().getSimpleName());
        boolean addedGenericStart = false;
        for (int i = 0; i < allTraits.size(); i++) {
            TraitElement elem = allTraits.get(i);
            if (elem.hasTypeParameters()) {
                if (!addedGenericStart) {
                    builder.append("<");
                    addedGenericStart = true;
                } else {
                    builder.append(", ");
                }
                elem.emitParametrizedTypeList(builder, true);
            }
        }
        if (addedGenericStart)
            builder.append(">");
        String desiredSuperclass = cls.getDesiredSuperclass().toString();
        if (!Utils.OBJECT_CLASS_NAME.equals(desiredSuperclass))
            builder.append(" extends ").append(cls.getDesiredSuperclass().getSimpleName());
        if (allTraits.size() > 0) {
            builder.append(" implements ");
            for (int i = 0; i < allTraits.size(); i++) {
                TraitElement elem = allTraits.get(i);
                elem.emitParametrizedInterfaceName(builder, false);
                if (i < allTraits.size() - 1)
                    builder.append(", ");
            }
        }

        builder.append(" {\n");

        emitDelegateFields(builder);
        emitInitMethod(builder);
        emitDelegateMethods(builder);

        builder.append("}");
    }

    private void emitDelegateFields(StringBuilder builder) {
        for (TraitElement elem : allTraits) {
            ClassName delegateClass = cls.getDelegateClassNameForTraitElement(elem);
            builder.append("\tprivate ").append(delegateClass.getSimpleName());
            if (elem.hasTypeParameters()) {
                builder.append("<");
                elem.emitParametrizedTypeList(builder, false);
                builder.append(">");
            }
            builder.append(" ")
            .append(getDelegateVariableName(elem)).append(";\n");
        }
        builder.append("\n");
    }

    private void emitInitMethod(StringBuilder builder) {
        builder.append("\tprotected final void init() {\n");
        for (TraitElement elem : allTraits) {
            ClassName delegateClass = cls.getDelegateClassNameForTraitElement(elem);
            builder.append("\t\t").append(getDelegateVariableName(elem));
            builder.append(" = new ").append(delegateClass.getSimpleName());
            if (elem.hasTypeParameters()) {
                builder.append("<");
                elem.emitParametrizedTypeList(builder, false);
                builder.append(">");
            }
            builder.append("(this);\n");
        }
        builder.append("\t}\n\n");
    }

    private String getDelegateVariableName(TraitElement elem) {
        String base = elem.getSimpleName();
        return base.substring(0, 1).toLowerCase() + base.substring(1) + ClassWithTraits.DELEGATE_SUFFIX;
    }

    private void emitDelegateMethods(StringBuilder builder) {
        Set<String> dupes = new HashSet<String>();
        Map<String, List<Pair<TraitElement, ExecutableElement>>> methodToExecElements = new HashMap<String, List<Pair<TraitElement, ExecutableElement>>>();
        for (TraitElement elem : allTraits) {
            List<? extends ExecutableElement> execElems = elem.getDeclaredMethods();
            for (ExecutableElement exec : execElems) {
                String signature = Utils.getMethodSignature(exec);
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
            for (String dup : dupes) {
                String simpleMethodName = Utils.getMethodNameFromSignature(dup);
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
            List<String> argNames = Utils.emitMethodSignature(builder, exec, null, elem.getSimpleName(), isAbstract, false);
            if (isAbstract) {
                builder.append(";\n\n");
            } else {
                String delegateVariableName = getDelegateVariableName(elem);
                builder.append(" {\n")
                .append("\t\tif (").append(delegateVariableName).append(" == null)\n")
                .append("\t\t\tthrow new IllegalStateException(\"init() not called on instance of class \" + getClass());\n")
                .append("\t\t");
                if (exec.getReturnType().getKind() != TypeKind.VOID)
                    builder.append("return ");
                builder.append(delegateVariableName)
                .append(".").append("default__").append(exec.getSimpleName()).append("(");
                for (int i = 0; i < argNames.size(); i++) {
                    builder.append(argNames.get(i));
                    if (i < argNames.size() - 1)
                        builder.append(", ");
                }
                builder.append(");\n");
                builder.append("\t}\n\n");
            }
        }
    }
}
