package com.yahoo.javatraits.processor;

import com.yahoo.javatraits.processor.data.TypeElementWrapper;
import com.yahoo.javatraits.processor.utils.TraitProcessorUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

public abstract class JavaTraitsProcessor<T extends TypeElementWrapper> extends AbstractProcessor {

    protected Messager messager;
    protected TraitProcessorUtils utils;
    protected Filer filer;
    
    protected abstract Class<? extends Annotation> getAnnotationClass();
    protected abstract T itemTypeFromTypeElement(TypeElement typeElem);
    protected abstract void processItem(T item);

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(getAnnotationClass().getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_6;
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        this.messager = env.getMessager();
        this.filer = env.getFiler();
        this.utils = new TraitProcessorUtils(messager, env.getTypeUtils());
    }
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Set<? extends Element> annotatedElements = env.getElementsAnnotatedWith(getAnnotationClass());
        processElements(annotatedElements);
        return true;
    }

    private void processElements(Set<? extends Element> elements) {
        for (Element e : elements) {
            if (e.getKind() != ElementKind.CLASS || !(e instanceof TypeElement)) {
                messager.printMessage(Kind.ERROR, "Only a class can be annotated with @" + getAnnotationClass().getSimpleName(), e);
            } else {
                TypeElement typeElem = (TypeElement) e;
                processItem(itemTypeFromTypeElement(typeElem));
            }
        }
    }

}
