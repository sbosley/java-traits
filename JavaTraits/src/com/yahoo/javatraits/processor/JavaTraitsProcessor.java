package com.yahoo.javatraits.processor;

import com.yahoo.javatraits.processor.data.TypeElementWrapper;
import com.yahoo.javatraits.processor.utils.TraitProcessorAptUtils;

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
    protected TraitProcessorAptUtils utils;
    protected Filer filer;
    
    protected abstract Class<? extends Annotation> getAnnotationClass();
    protected abstract T itemFromTypeElement(TypeElement typeElem);
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
        this.utils = new TraitProcessorAptUtils(messager, env.getTypeUtils());
    }
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        try {
            Set<? extends Element> annotatedElements = env.getElementsAnnotatedWith(getAnnotationClass());
            processElements(annotatedElements);
        } catch (Exception e) {
            messager.printMessage(Kind.ERROR, "Uncaught exception in annotation processor " + this + ": " + e + ", message " + e.getMessage());
            throw new RuntimeException(e);
        }
        return true;
    }

    private void processElements(Set<? extends Element> elements) {
        for (Element e : elements) {
            if (e.getKind() != ElementKind.CLASS || !(e instanceof TypeElement)) {
                messager.printMessage(Kind.ERROR, "Only a class can be annotated with @" + getAnnotationClass().getSimpleName(), e);
            } else {
                processItem(itemFromTypeElement((TypeElement) e));
            }
        }
    }

}
