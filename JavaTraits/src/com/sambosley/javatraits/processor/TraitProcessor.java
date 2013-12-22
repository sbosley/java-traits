package com.sambosley.javatraits.processor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import com.sambosley.javatraits.annotations.HasTraits;
import com.sambosley.javatraits.annotations.Trait;
import com.sambosley.javatraits.utils.FullyQualifiedName;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
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
		Set<ClassWithTraits> classesWithTraits = getClassesWithTraits(env);
		Map<FullyQualifiedName, TraitElement> interfaceNames = getTraitElements(env);

		generateTraitInterfaces(interfaceNames);
		generateTraitDelegates(classesWithTraits, interfaceNames);
		generateTraitImplementingSuperclasses(classesWithTraits, interfaceNames);
		return true;
	}
	
	private Map<FullyQualifiedName, TraitElement> getTraitElements(RoundEnvironment env) {
		Map<FullyQualifiedName, TraitElement> result = new HashMap<FullyQualifiedName, TraitElement>();
		Set<? extends Element> traitElements = env.getElementsAnnotatedWith(Trait.class);
		for (Element e : traitElements) {
			if (e.getKind() != ElementKind.CLASS)
				messager.printMessage(Kind.ERROR, "Only a class can be annotated with @Trait", e);
			else {
				TypeElement typeElem = (TypeElement) e;
				TraitElement traitElement = new TraitElement(typeElem, messager);
				result.put(traitElement.fqn, traitElement);
			}
		}
		return result;
	}
	
	private Set<ClassWithTraits> getClassesWithTraits(RoundEnvironment env) {
		Set<? extends Element> elementsWithTraits = env.getElementsAnnotatedWith(HasTraits.class);
		Set<ClassWithTraits> result = new HashSet<ClassWithTraits>();
		for (Element e : elementsWithTraits) {
			if (e.getKind() != ElementKind.CLASS)
				messager.printMessage(Kind.ERROR, "Only a class can be annotated with @Trait", e);
			else {
				TypeElement typeElem = (TypeElement) e;
				result.add(new ClassWithTraits(typeElem, messager));
			}
		}
		return result;
	}
	
	private void generateTraitInterfaces(Map<FullyQualifiedName, TraitElement> traitElements) {
		for (TraitElement te : traitElements.values()) {
			new TraitInterfaceWriter(te, messager).writeInterface(filer);
		}
	}
	
	private void generateTraitDelegates(Set<ClassWithTraits> classesWithTraits, Map<FullyQualifiedName, TraitElement> traitInterfaceMap) {
		for (ClassWithTraits cls : classesWithTraits) {
			List<FullyQualifiedName> allTraits = cls.getTraitClasses();
			for (FullyQualifiedName fqn : allTraits) {
				TraitElement correspondingTrait = traitInterfaceMap.get(fqn);
				if (correspondingTrait == null)
					messager.printMessage(Kind.ERROR, "Couldn't find TraitElement for name " + fqn.toString());
				new TraitDelegateWriter(cls, correspondingTrait, messager).writeDelegate(filer);
			}
		}
	}
	
	private void generateTraitImplementingSuperclasses(Set<ClassWithTraits> classesWithTraits, Map<FullyQualifiedName, TraitElement> traitInterfaceMap) {
		for (ClassWithTraits cls : classesWithTraits) {
			new ClassWithTraitsSuperclassWriter(cls, traitInterfaceMap, messager).writeClass(filer);
		}
	}
}
