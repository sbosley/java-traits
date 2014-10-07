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

import com.yahoo.annotations.utils.Utils;

public abstract class JavaTraitsProcessor extends AbstractProcessor {

    protected Messager messager;
    protected Utils utils;
    protected Filer filer;
    
    protected Set<? extends Element> elements = null;
    
    protected abstract Class<? extends Annotation> getAnnotationClass();
    protected abstract void processElements();
    
    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        this.messager = env.getMessager();
        this.filer = env.getFiler();
        this.utils = new Utils(messager, env.getTypeUtils());
    }
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        initializeElements(env);
        
        if (elements != null) {
            processElements();
        }
        
        return true;
    }
    
    private void initializeElements(RoundEnvironment env) {
        if (elements == null) {
            Set<? extends Element> foundElements = env.getElementsAnnotatedWith(getAnnotationClass());
            if (foundElements.size() > 0) {
                this.elements = foundElements;
            }
        }
    }

}
