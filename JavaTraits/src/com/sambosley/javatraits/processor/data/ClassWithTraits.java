/**
 * Copyright 2014 Sam Bosley
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.sambosley.javatraits.processor.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import com.sambosley.javatraits.annotations.HasTraits;
import com.sambosley.javatraits.utils.FullyQualifiedName;
import com.sambosley.javatraits.utils.Utils;

public class ClassWithTraits extends TypeElementWrapper {

    public static final String GEN_SUFFIX = "Gen";
    public static final String DELEGATE_SUFFIX = "Delegate";

    private List<TraitElement> traitClasses;
    private FullyQualifiedName desiredSuperclass;
    private FullyQualifiedName generatedSuperclass;
    private Map<String, FullyQualifiedName> prefer;

    public ClassWithTraits(TypeElement elem, Messager messager, Map<FullyQualifiedName, TraitElement> traitMap) {
        super(elem, messager);
        initTraitClasses(traitMap);
        initSuperclasses();
        initPreferValues();
    }

    private void initTraitClasses(final Map<FullyQualifiedName, TraitElement> traitMap) {
        List<FullyQualifiedName> traitNames = Utils.getClassValuesFromAnnotation(HasTraits.class, elem, "traits", messager);
        traitClasses = Utils.map(traitNames, new Utils.MapFunction<FullyQualifiedName, TraitElement>() {
            @Override
            public TraitElement map(FullyQualifiedName arg) {
                TraitElement correspondingTrait = traitMap.get(arg);
                if (correspondingTrait == null)
                    messager.printMessage(Kind.ERROR, "Couldn't find TraitElement for name " + arg.toString());
                return correspondingTrait;
            }
        });
    }

    private void initSuperclasses() {
        List<FullyQualifiedName> desiredSuperclassResult = Utils.getClassValuesFromAnnotation(HasTraits.class, elem, "desiredSuperclass", messager); 
        desiredSuperclass = desiredSuperclassResult.size() > 0 ? desiredSuperclassResult.get(0) : new FullyQualifiedName("java.lang.Object");
        generatedSuperclass = new FullyQualifiedName(fqn.toString() + GEN_SUFFIX);
    }
    
    private void initPreferValues() {
        prefer = new HashMap<String, FullyQualifiedName>();
        AnnotationMirror hasTraits = Utils.findAnnotationMirror(elem, HasTraits.class);
        AnnotationValue preferValue = Utils.findAnnotationValue(hasTraits, "prefer");
        if (preferValue != null && preferValue.getValue() instanceof List) {
            @SuppressWarnings("unchecked")
            List<? extends AnnotationValue> preferList = (List<? extends AnnotationValue>) preferValue.getValue();
            for (AnnotationValue entry : preferList) {
                Object value = entry.getValue();
                if (value instanceof AnnotationMirror) {
                    AnnotationMirror preferMirror = (AnnotationMirror) value;
                    AnnotationValue targetValue = Utils.findAnnotationValue(preferMirror, "target");
                    AnnotationValue methodValue = Utils.findAnnotationValue(preferMirror, "method");
                    
                    FullyQualifiedName targetName = Utils.getClassValuesFromAnnotationValue(targetValue).get(0);
                    String method = (String) methodValue.getValue();
                    prefer.put(method, targetName);
                }
            }
        }
    }

    public FullyQualifiedName getFullyQualifiedGeneratedSuperclassName() {
        return generatedSuperclass;
    }

    public List<TraitElement> getTraitClasses() {
        return traitClasses;
    }

    public FullyQualifiedName getDesiredSuperclass() {
        return desiredSuperclass;
    }
    
    public Map<String, FullyQualifiedName> getPreferMap() {
        return prefer;
    }

    public FullyQualifiedName getDelegateClassNameForTraitElement(TraitElement traitElement) {
        return new FullyQualifiedName(traitElement.getFullyQualifiedName() + "__" + getSimpleName() + DELEGATE_SUFFIX);
    }
    
    public void emitParametrizedTypeList(StringBuilder builder, boolean appendBounds) {
        emitParametrizedTypeList(builder, null, appendBounds);
    }
    
    public void emitParametrizedTypeList(StringBuilder builder, TraitElement onlyForThisElem, boolean appendBounds) {
        boolean addedParameterStart = false;
        for (int i = 0; i < traitClasses.size(); i++) {
            TraitElement elem = traitClasses.get(i);
            if (elem.hasTypeParameters()) {
                if (!addedParameterStart) {
                    builder.append("<");
                    addedParameterStart = true;
                }
                if (onlyForThisElem != null && !onlyForThisElem.getFullyQualifiedName().equals(elem.getFullyQualifiedName())) {
                    int paramCount = elem.getTypeParameters().size();
                    for (int p = 0; p < paramCount; p++) {
                        builder.append("?");
                        if (p < paramCount - 1)
                            builder.append(", ");
                    }
                } else {
                    elem.emitParametrizedTypeList(builder, appendBounds);
                }
            }
            if (i < traitClasses.size() - 1 && addedParameterStart)
                builder.append(", ");
        }
        if (addedParameterStart)
            builder.append(">");
    }
}
