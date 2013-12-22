package com.sambosley.javatraits.processor;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import com.sambosley.javatraits.annotations.HasTraits;
import com.sambosley.javatraits.annotations.Trait;

public class TraitProcessor extends AbstractProcessor {

	private Messager messager;
	private Filer filer;
	
	@Override
	public synchronized void init(ProcessingEnvironment env) {
		super.init(env);
		
		this.messager = env.getMessager();
		this.filer = env.getFiler();
	}
	
	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> supportedTypes = new LinkedHashSet<String>();
		supportedTypes.add(Trait.class.getCanonicalName());
		supportedTypes.add(HasTraits.class.getCanonicalName());
		return supportedTypes;
	}
	
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
		Map<String, String> interfaceNames = generateTraitInterfaces(env);
		return true;
	}
	
	private Map<String, String> generateTraitInterfaces(RoundEnvironment env) {
		Map<String, String> result = new HashMap<String, String>();
		Set<? extends Element> traitElements = env.getElementsAnnotatedWith(Trait.class);
		for (Element e : traitElements) {
			if (e.getKind() != ElementKind.CLASS)
				messager.printMessage(Kind.ERROR, "Only a class can be annotated with @Trait", e);
			else {
				TypeElement typeElem = (TypeElement) e;
				TraitElement traitElement = new TraitElement(typeElem, messager);
				String typeName = typeElem.getQualifiedName().toString();
				result.put(typeName, traitElement.getFullyQualifiedInterfaceName());
				new TraitInterfaceWriter(traitElement, messager).writeInterface(filer);
			}
		}
		return result;
	}
}
