/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor;

import com.yahoo.javatraits.annotations.Trait;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.processor.writers.TraitDelegateWriter;
import com.yahoo.javatraits.processor.writers.TraitInterfaceWriter;

import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;

public class TraitProcessor extends JavaTraitsProcessor<TraitElement> {

    @Override
    protected Class<? extends Annotation> getAnnotationClass() {
        return Trait.class;
    }

    @Override
    protected TraitElement itemFromTypeElement(TypeElement typeElem) {
        return new TraitElement(typeElem, utils);
    }

    @Override
    protected void processItem(TraitElement item) {
        new TraitInterfaceWriter(item, utils).writeClass(filer);
        new TraitDelegateWriter(item, utils).writeClass(filer);
    }
}
