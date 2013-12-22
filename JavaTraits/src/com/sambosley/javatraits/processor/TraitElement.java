package com.sambosley.javatraits.processor;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import com.sambosley.javatraits.utils.Pair;
import com.sambosley.javatraits.utils.Utils;

public class TraitElement {

	public static final String INTERFACE_SUFFIX = "Interface";
	
	private TypeElement elem;
	private Messager messager;
	private String fullyQualifiedName;
	private Pair<String, String> nameElements;
	private List<ExecutableElement> declaredMethods;
	
	public TraitElement(TypeElement elem, Messager messager) {
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
					messager.printMessage(Kind.ERROR, "Trait elements may only declare methods or abstract methods", e);
				}
			else
				declaredMethods.add((ExecutableElement) e);
		}
	}
	
	public TypeElement getSourceElement() {
		return elem;
	}
	
	public String getFullyQualifiedInterfaceName() {
		return fullyQualifiedName + INTERFACE_SUFFIX;
	}
	
	public String getSimpleInterfaceName() {
		return getSimpleName() + INTERFACE_SUFFIX;
	}

	public String getPackageName() {
		return nameElements.getLeft();
	}
	
	public String getSimpleName() {
		return nameElements.getRight();
	}
	
	public List<? extends ExecutableElement> getDeclaredMethods() {
		return declaredMethods;
	}
	
}
