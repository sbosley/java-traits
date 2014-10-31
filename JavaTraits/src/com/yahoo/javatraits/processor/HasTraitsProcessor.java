package com.yahoo.javatraits.processor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import com.yahoo.javatraits.annotations.HasTraits;
import com.yahoo.javatraits.processor.data.ClassWithTraits;
import com.yahoo.javatraits.processor.writers.ClassWithTraitsSuperclassWriter;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes(value="com.yahoo.javatraits.annotations.HasTraits")
public class HasTraitsProcessor extends JavaTraitsProcessor {

    @Override
    protected Class<? extends Annotation> getAnnotationClass() {
        return HasTraits.class;
    }
    
    @Override
    protected void processElements(Set<? extends Element> elements) {
        List<ClassWithTraits> classesWithTraits = getClassesWithTraits(elements);
        
        generateTraitImplementingSuperclasses(classesWithTraits);
    }
    
    private List<ClassWithTraits> getClassesWithTraits(Set<? extends Element> elements) {
        List<ClassWithTraits> result = new ArrayList<ClassWithTraits>();
        for (Element e : elements) {
            if (e.getKind() != ElementKind.CLASS) {
                messager.printMessage(Kind.ERROR, "Only a class can be annotated with @HasTraits", e);
            } else {
                TypeElement typeElem = (TypeElement) e;
                result.add(new ClassWithTraits(typeElem, utils));
            }
        }
        return result;
    }
    
    private void generateTraitImplementingSuperclasses(List<ClassWithTraits> classesWithTraits) {
        for (ClassWithTraits cls : classesWithTraits) {
            new ClassWithTraitsSuperclassWriter(cls, utils).writeClass(filer);
        }
    }
}
