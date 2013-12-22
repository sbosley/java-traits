package com.sambosley.javatraits.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.sambosley.javatraits.utils.FullyQualifiedName;
import com.sambosley.javatraits.utils.Utils;

public class TraitDelegateWriter {

	private ClassWithTraits cls;
	private TraitElement traitElement;
	private Messager messager;
	private FullyQualifiedName traitDelegateClass;
	private FullyQualifiedName delegateClass;
	
	private static final String DELEGATE_SUFFIX = "Delegate";
	
	public TraitDelegateWriter(ClassWithTraits cls, TraitElement traitElement, Messager messager) {
		this.cls = cls;
		this.traitElement = traitElement;
		this.messager = messager;
		this.traitDelegateClass = new FullyQualifiedName(traitElement.getFullyQualifiedName() + "$" + cls.getSimpleName() + DELEGATE_SUFFIX);
		this.delegateClass = cls.getFullyQualifiedGeneratedSuperclassName();
	}
	
	public void writeDelegate(Filer filer) {
		try {
			JavaFileObject jfo = filer.createSourceFile(traitDelegateClass.toString(), cls.getSourceElement());
			Writer writer = jfo.openWriter();
			writer.write(emitDelegate());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			messager.printMessage(Kind.ERROR, "IOException writing delegate class with delegate " + 
					cls.getSimpleName() + " for trait", cls.getSourceElement());
		}
	}
	
	private String emitDelegate() {
		StringBuilder builder = new StringBuilder();
		emitPackage(builder);
		emitImports(builder);
		emitDelegateDeclaration(builder);
		return builder.toString();
	}
	
	private void emitPackage(StringBuilder builder) {
		builder.append("package ").append(traitDelegateClass.getPackageName()).append(";\n\n");
	}
	
	private void emitImports(StringBuilder builder) {
		Set<String> necessaryImports = Utils.generateImportsFromExecutableElements(traitElement.getAbstractMethods(), messager);
		necessaryImports.add(delegateClass.toString());
		for (String s : necessaryImports) {
			builder.append("import ").append(s).append(";\n");
		}
		builder.append("\n");
	}
	
	private void emitDelegateDeclaration(StringBuilder builder) {
		builder.append("public class ").append(traitDelegateClass.getSimpleName())
		.append(" extends ").append(traitElement.getSimpleName()).append(" {\n\n");
		
		emitDelegateInstance(builder);
		emitConstructor(builder);
		emitAbstractMethodImplementations(builder);
		
		builder.append("}");
	}
	
	private void emitDelegateInstance(StringBuilder builder) {
		builder.append("\tprivate ").append(delegateClass.getSimpleName()).append(" delegate;\n\n");
	}
	
	private void emitConstructor(StringBuilder builder) {
		builder.append("\tpublic ").append(traitDelegateClass.getSimpleName())
		.append("(").append(delegateClass.getSimpleName()).append(" delegate) {\n")
		.append("\t\tthis.delegate = delegate;\n")
		.append("}\n\n");
	}
	
	private void emitAbstractMethodImplementations(StringBuilder builder) {
		List<? extends ExecutableElement> abstractMethods = traitElement.getAbstractMethods();
		for (ExecutableElement exec : abstractMethods) {
			List<String> argNames = Utils.emitMethodSignature(builder, exec);
			builder.append(" {\n");
			builder.append("\t\tdelegate.").append(exec.getSimpleName().toString()).append("(");
			for (int i = 0; i < argNames.size(); i++) {
				builder.append(argNames.get(i));
				if (i < argNames.size() - 1)
					builder.append(", ");
			}
			builder.append(");\n")
			.append("\t}\n\n");
		}
	}
	
}
