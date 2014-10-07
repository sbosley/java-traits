/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import com.yahoo.javatraits.annotations.Trait;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.processor.writers.TraitDelegateWriter;
import com.yahoo.javatraits.processor.writers.TraitInterfaceWriter;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes(value="com.yahoo.javatraits.annotations.Trait")
public class TraitProcessor extends JavaTraitsProcessor {

    @Override
    protected Class<? extends Annotation> getAnnotationClass() {
        return Trait.class;
    }

    @Override
    protected void processElements() {
        List<TraitElement> traitMap = getTraitElements();
        
        generateTraitInterfaces(traitMap);
        generateTraitDelegates(traitMap);
    }

    private List<TraitElement> getTraitElements() {
        List<TraitElement> result = new ArrayList<TraitElement>();

        for (Element e : elements) {
            if (e.getKind() != ElementKind.CLASS) {
                messager.printMessage(Kind.ERROR, "Only a class can be annotated with @Trait", e);
            } else {
                TypeElement typeElem = (TypeElement) e;
                TraitElement traitElement = new TraitElement(typeElem, utils);
                result.add(traitElement);
            }
        }
        return result;
    }

    private void generateTraitInterfaces(List<TraitElement> traitElements) {
        for (TraitElement te : traitElements) {
            new TraitInterfaceWriter(te, utils).writeInterface(filer);
        }
    }

    private void generateTraitDelegates(List<TraitElement> traitElements) {
        for (TraitElement trait : traitElements) {
            new TraitDelegateWriter(trait, utils).writeDelegate(filer);
        }
    }
}
