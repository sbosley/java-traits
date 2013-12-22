package com.sambosley.javatraits.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.sambosley.javatraits.processor.visitors.ImportGatheringTypeVisitor;
import com.sambosley.javatraits.utils.Pair;
import com.sambosley.javatraits.utils.Utils;

public class TraitInterfaceGenerator {

	public static final String INTERFACE_SUFFIX = "Interface";
	
	private TypeElement elem;
	private Messager messager;
	private String fullyQualifiedName;
	private Pair<String, String> nameElements;
	private List<ExecutableElement> declaredMethods;
	
	public TraitInterfaceGenerator(TypeElement elem, Messager messager) {
		this.elem = elem;
		this.messager = messager;
		this.fullyQualifiedName = elem.getQualifiedName().toString();
		this.nameElements = Utils.splitFullyQualifiedName(fullyQualifiedName);
		validateElement();
	}
	
	private void validateElement() {
		declaredMethods = new ArrayList<ExecutableElement>();
		List<? extends Element> enclosedElements = elem.getEnclosedElements();
		for (Element e : enclosedElements) {
			if (e.getKind() != ElementKind.METHOD || !(e instanceof ExecutableElement))
				if (e.getKind() == ElementKind.CONSTRUCTOR && (e instanceof ExecutableElement)) {
					if (((ExecutableElement) e).getParameters().size() > 0)
						messager.printMessage(Kind.ERROR, "Trait constructors cannot have arguments", e);
				} else {					
					messager.printMessage(Kind.ERROR, "Trait elements may only declare methods or abstract methods, " + e.getKind() + ", " + e.toString(), e);
				}
			else
				declaredMethods.add((ExecutableElement) e);
		}
	}
	
	public String getInterfaceName() {
		return fullyQualifiedName + INTERFACE_SUFFIX;
	}
	
	public void writeInterface(Filer filer) {
		try {
			JavaFileObject jfo = filer.createSourceFile(getInterfaceName(), elem);
			Writer writer = jfo.openWriter();
			writer.write(emitInterface());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			messager.printMessage(Kind.ERROR, "IOException writing interface for trait", elem);
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
		builder.append("package ").append(nameElements.getLeft()).append(";\n\n");
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
		for (ExecutableElement exec : declaredMethods) {
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
		.append(Utils.getSimpleNameFromFullyQualifiedName(getInterfaceName())).append(" {\n");
		emitMethodDeclarations(builder);
		builder.append("}");
	}
	
	private void emitMethodDeclarations(StringBuilder builder) {
		for (ExecutableElement exec : declaredMethods) {
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
