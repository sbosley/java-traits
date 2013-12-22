package com.sambosley.javatraits.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

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
		Set<String> necessaryImports = Utils.generateImportsFromExecutableElements(element.getDeclaredMethods(), messager);
		for (String s : necessaryImports) {
			builder.append("import ").append(s).append(";\n");
		}
		builder.append("\n");
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
		Utils.emitMethodSignature(builder, exec);
		builder.append(";\n");
	}
	
}
