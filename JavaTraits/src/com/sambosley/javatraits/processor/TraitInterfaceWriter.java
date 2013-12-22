package com.sambosley.javatraits.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.sambosley.javatraits.processor.visitors.ImportGatheringTypeVisitor;
import com.sambosley.javatraits.utils.Utils;

public class TraitInterfaceWriter {

	private final TraitElement element;
	private Messager messager;
	
	public TraitInterfaceWriter(TraitElement element, Messager messager) {
		this.element = element;
		this.messager = messager;
	}
	
	public void writeInterface(Filer filer) {
		try {
			JavaFileObject jfo = filer.createSourceFile(element.getFullyQualifiedInterfaceName(), element.getSourceElement());
			Writer writer = jfo.openWriter();
			writer.write(emitInterface());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			messager.printMessage(Kind.ERROR, "IOException writing interface for trait", element.getSourceElement());
		}
	}
	
	private String emitInterface() {
		StringBuilder builder = new StringBuilder();
		emitPackage(builder);
		emitImports(builder);
		emitInterfaceDeclaration(builder);
		return builder.toString();
	}
	
	private void emitPackage(StringBuilder builder) {
		builder.append("package ").append(element.getPackageName()).append(";\n\n");
	}
	
	private void emitImports(StringBuilder builder) {
		Set<String> necessaryImports = gatherImports();
		for (String s : necessaryImports) {
			builder.append("import ").append(s).append(";\n");
		}
		builder.append("\n");
	}
	
	private Set<String> gatherImports() {
		Set<String> imports = new HashSet<String>();
		for (ExecutableElement exec : element.getDeclaredMethods()) {
			ImportGatheringTypeVisitor visitor = new ImportGatheringTypeVisitor(exec, messager);
			TypeMirror returnType = exec.getReturnType();
			returnType.accept(visitor, imports);
			List<? extends VariableElement> parameters = exec.getParameters();
			for (VariableElement var : parameters) {
				var.asType().accept(visitor, imports);
			}
		}
		
		return imports;
	}
	
	private void emitInterfaceDeclaration(StringBuilder builder) {
		builder.append("public interface ")
		.append(element.getSimpleInterfaceName()).append(" {\n");
		emitMethodDeclarations(builder);
		builder.append("}");
	}
	
	private void emitMethodDeclarations(StringBuilder builder) {
		for (ExecutableElement exec : element.getDeclaredMethods()) {
			emitMethodDeclarationForExecutableElement(builder, exec);
		}
	}
	
	private void emitMethodDeclarationForExecutableElement(StringBuilder builder, ExecutableElement exec) {
		builder.append("\tpublic ");
		builder.append(Utils.getSimpleNameFromFullyQualifiedName(exec.getReturnType().toString()))
		.append(" ").append(exec.getSimpleName().toString())
		.append("(");
		List<? extends VariableElement> parameters = exec.getParameters();
		for (int i = 0; i < parameters.size(); i++) {
			VariableElement var = parameters.get(i);
			String typeString = Utils.getSimpleNameFromFullyQualifiedName(var.asType().toString());
			builder.append(typeString).append(" ").append(var.toString());
			if (i < parameters.size() - 1)
				builder.append(", ");
		}
		builder.append(");\n");
	}
	
}
