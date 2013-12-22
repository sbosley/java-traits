package com.sambosley.javatraits.processor.writers;

import java.io.IOException;
import java.io.Writer;
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
import com.sambosley.javatraits.utils.FullyQualifiedName;
import com.sambosley.javatraits.utils.Utils;

public class ClassWithTraitsSuperclassWriter {

    private ClassWithTraits cls;
    private Map<FullyQualifiedName, TraitElement> traitElementMap;
    private Messager messager;
    private List<TraitElement> allTraits;

    private static final String OBJECT_CLASS_NAME = "java.lang.Object";

    public ClassWithTraitsSuperclassWriter(ClassWithTraits cls, Map<FullyQualifiedName, TraitElement> traitElementMap, Messager messager) {
        this.cls = cls;
        this.traitElementMap = traitElementMap;
        this.messager = messager;
        this.allTraits = Utils.map(Utils.getClassFromAnnotation(HasTraits.class, cls.getSourceElement(), "traits", messager),
                new Utils.MapFunction<FullyQualifiedName, TraitElement>() {
            public TraitElement map(FullyQualifiedName fqn) {
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
        if (!OBJECT_CLASS_NAME.equals(desiredSuperclass))
            imports.add(desiredSuperclass);

        for (String s : imports) {
            builder.append("import ").append(s).append(";\n");
        }
        builder.append("\n");
    }

    private void emitClassDeclaration(StringBuilder builder) {
        builder.append("public abstract class ").append(cls.getFullyQualifiedGeneratedSuperclassName().getSimpleName());
        String desiredSuperclass = cls.getDesiredSuperclass().toString();
        if (!OBJECT_CLASS_NAME.equals(desiredSuperclass))
            builder.append(" extends ").append(cls.getDesiredSuperclass().getSimpleName());
        if (allTraits.size() > 0) {
            builder.append(" implements ");
            for (int i = 0; i < allTraits.size(); i++) {
                TraitElement elem = allTraits.get(i);
                builder.append(elem.getSimpleInterfaceName());
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
            FullyQualifiedName delegateClass = cls.getDelegateClassNameForTraitElement(elem);
            builder.append("\tprivate ").append(delegateClass.getSimpleName()).append(" ")
            .append(getDelegateVariableName(elem)).append(";\n");
        }
        builder.append("\n");
    }

    private void emitInitMethod(StringBuilder builder) {
        builder.append("\tprotected void init() {\n");
        for (TraitElement elem : allTraits) {
            builder.append("\t\t").append(getDelegateVariableName(elem))
            .append(" = new ").append(cls.getDelegateClassNameForTraitElement(elem).getSimpleName())
            .append("(this);\n");
        }
        builder.append("\t}\n\n");
    }

    private String getDelegateVariableName(TraitElement elem) {
        String base = elem.getSimpleName();
        return base.substring(0, 1).toLowerCase() + base.substring(1) + ClassWithTraits.DELEGATE_SUFFIX;
    }

    private void emitDelegateMethods(StringBuilder builder) {
        Set<String> alreadyUsedSignatures = new HashSet<String>();
        Set<String> dupes = new HashSet<String>();
        for (TraitElement elem : allTraits) {
            List<? extends ExecutableElement> execElems = elem.getDeclaredMethods();
            for (ExecutableElement exec : execElems) {
                if (alreadyUsedSignatures.contains(exec.toString())) {
                    dupes.add(exec.toString());
                } else {
                    Set<Modifier> modifiers = exec.getModifiers();
                    boolean isAbstract = modifiers.contains(Modifier.ABSTRACT);
                    List<String> argNames = Utils.emitMethodSignature(builder, exec, isAbstract);
                    if (isAbstract) {
                        builder.append(";\n\n");
                    } else {
                        builder.append(" {\n")
                        .append("\t\t");
                        if (exec.getReturnType().getKind() != TypeKind.VOID)
                            builder.append("return ");
                        builder.append(getDelegateVariableName(elem))
                        .append(".").append(exec.getSimpleName()).append("(");
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

    }

}
