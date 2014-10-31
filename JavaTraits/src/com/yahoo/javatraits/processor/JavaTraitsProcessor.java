package com.yahoo.javatraits.processor;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import com.yahoo.javatraits.processor.utils.TraitProcessorUtils;

public abstract class JavaTraitsProcessor extends AbstractProcessor {

    protected Messager messager;
    protected TraitProcessorUtils utils;
    protected Filer filer;
    
    protected abstract Class<? extends Annotation> getAnnotationClass();
    protected abstract void processElements(Set<? extends Element> elements);
    
    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        this.messager = env.getMessager();
        this.filer = env.getFiler();
        this.utils = new TraitProcessorUtils(messager, env.getTypeUtils());
    }
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        processElements(env.getElementsAnnotatedWith(getAnnotationClass()));
        return true;
    }

}
