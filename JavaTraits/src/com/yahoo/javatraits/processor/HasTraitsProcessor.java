package com.yahoo.javatraits.processor;

import com.yahoo.javatraits.annotations.HasTraits;
import com.yahoo.javatraits.processor.data.ClassWithTraits;
import com.yahoo.javatraits.processor.writers.ClassWithTraitsSuperclassWriter;

import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;

public class HasTraitsProcessor extends JavaTraitsProcessor<ClassWithTraits> {

    @Override
    protected Class<? extends Annotation> getAnnotationClass() {
        return HasTraits.class;
    }

    @Override
    protected ClassWithTraits itemFromTypeElement(TypeElement typeElem) {
        return new ClassWithTraits(typeElem, utils);
    }

    @Override
    protected void processItem(ClassWithTraits item) {
        new ClassWithTraitsSuperclassWriter(item, utils).writeClass(filer);
    }
}
