/**
 * Copyright 2014 Yahoo Inc.
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.sambosley.javatraits.processor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import com.sambosley.javatraits.annotations.HasTraits;
import com.sambosley.javatraits.annotations.Trait;
import com.sambosley.javatraits.processor.data.ClassWithTraits;
import com.sambosley.javatraits.processor.data.TraitElement;
import com.sambosley.javatraits.processor.writers.ClassWithTraitsSuperclassWriter;
import com.sambosley.javatraits.processor.writers.TraitDelegateWriter;
import com.sambosley.javatraits.processor.writers.TraitInterfaceWriter;
import com.sambosley.javatraits.utils.ClassName;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes(value="com.sambosley.javatraits.annotations.*")
public class TraitProcessor extends AbstractProcessor {

    private Messager messager;
    private Filer filer;
    
    private Set<? extends Element> traitElements = null;
    private Set<? extends Element> elementsWithTraits = null;
    private boolean finishedGeneratingFiles = false;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        this.messager = env.getMessager();
        this.filer = env.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        tryToInitTraitElements(env);
        tryToInitClassesWithTraits(env);
        
        if (traitElements != null && elementsWithTraits != null && !finishedGeneratingFiles) {
            Map<ClassName, TraitElement> traitMap = getTraitElements();
            Set<ClassWithTraits> classesWithTraits = getClassesWithTraits(traitMap);
            generateTraitInterfaces(traitMap);
            generateTraitDelegates(classesWithTraits, traitMap);
            generateTraitImplementingSuperclasses(classesWithTraits, traitMap);
            finishedGeneratingFiles = true;
        }

        
        return true;
    }
    
    private void tryToInitTraitElements(RoundEnvironment env) {
        if (traitElements == null) {
            Set<? extends Element> traitElements = env.getElementsAnnotatedWith(Trait.class);
            if (traitElements.size() > 0)
                this.traitElements = traitElements;
        }
    }
    
    private void tryToInitClassesWithTraits(RoundEnvironment env) {
        if (elementsWithTraits == null) {
            Set<? extends Element> elementsWithTraits = env.getElementsAnnotatedWith(HasTraits.class);
            if (elementsWithTraits.size() > 0)
                this.elementsWithTraits = elementsWithTraits;
        }
    }

    private Map<ClassName, TraitElement> getTraitElements() {
        Map<ClassName, TraitElement> result = new HashMap<ClassName, TraitElement>();
        
        for (Element e : traitElements) {
            if (e.getKind() != ElementKind.CLASS)
                messager.printMessage(Kind.ERROR, "Only a class can be annotated with @Trait", e);
            else {
                TypeElement typeElem = (TypeElement) e;
                TraitElement traitElement = new TraitElement(typeElem, messager);
                result.put(traitElement.getFullyQualifiedName(), traitElement);
            }
        }
        return result;
    }

    private Set<ClassWithTraits> getClassesWithTraits(Map<ClassName, TraitElement> traitMap) {
        Set<ClassWithTraits> result = new HashSet<ClassWithTraits>();
        for (Element e : elementsWithTraits) {
            if (e.getKind() != ElementKind.CLASS)
                messager.printMessage(Kind.ERROR, "Only a class can be annotated with @Trait", e);
            else {
                TypeElement typeElem = (TypeElement) e;
                result.add(new ClassWithTraits(typeElem, messager, traitMap));
            }
        }
        return result;
    }

    private void generateTraitInterfaces(Map<ClassName, TraitElement> traitElements) {
        for (TraitElement te : traitElements.values()) {
            new TraitInterfaceWriter(te, messager).writeInterface(filer);
        }
    }

    private void generateTraitDelegates(Set<ClassWithTraits> classesWithTraits, Map<ClassName, TraitElement> traitInterfaceMap) {
        for (ClassWithTraits cls : classesWithTraits) {
            List<TraitElement> allTraits = cls.getTraitClasses();
            for (TraitElement trait : allTraits) {
                new TraitDelegateWriter(cls, trait, messager).writeDelegate(filer);
            }
        }
    }

    private void generateTraitImplementingSuperclasses(Set<ClassWithTraits> classesWithTraits, Map<ClassName, TraitElement> traitInterfaceMap) {
        for (ClassWithTraits cls : classesWithTraits) {
            new ClassWithTraitsSuperclassWriter(cls, traitInterfaceMap, messager).writeClass(filer);
        }
    }
}
