/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

import com.yahoo.annotations.model.ClassName;
import com.yahoo.annotations.model.GenericName;
import com.yahoo.annotations.utils.Utils;
import com.yahoo.javatraits.annotations.HasTraits;

public class ClassWithTraits extends TypeElementWrapper {

    public static final String GEN_SUFFIX = "Gen";

    private List<TraitElement> traitClasses;

    private ClassName desiredSuperclass;

    private ClassName generatedSuperclass;
    private Map<String, ClassName> prefer;

    public ClassWithTraits(TypeElement elem, Utils utils, Map<ClassName, TraitElement> traitMap) {
        super(elem, utils);
        initTraitClasses(traitMap);
        initSuperclasses();
        initPreferValues();
    }

    private void initTraitClasses(final Map<ClassName, TraitElement> traitMap) {
        List<ClassName> traitNames = Utils.getClassValuesFromAnnotation(HasTraits.class, elem, "traits");
        traitClasses = Utils.map(traitNames, new Utils.MapFunction<ClassName, TraitElement>() {
            @Override
            public TraitElement map(ClassName arg) {
                TraitElement correspondingTrait = traitMap.get(arg);
                if (correspondingTrait == null) {
                    utils.getMessager().printMessage(Kind.ERROR, "Couldn't find TraitElement for name " + arg.toString());
                }
                return correspondingTrait;
            }
        });
    }

    private void initSuperclasses() {
        AnnotationMirror hasTraits = Utils.findAnnotationMirror(elem, HasTraits.class);
        AnnotationValue desiredSuperclassValue = Utils.findAnnotationValue(hasTraits, "desiredSuperclass");
        if (desiredSuperclassValue != null) {
            Object value = desiredSuperclassValue.getValue();
            if (value instanceof AnnotationMirror) {
                AnnotationMirror desiredSuperclassMirror = (AnnotationMirror) value;
                AnnotationValue superclassValue = Utils.findAnnotationValue(desiredSuperclassMirror, "superclass");

                List<ClassName> superclassNames = Utils.getClassValuesFromAnnotationValue(superclassValue);
                desiredSuperclass = superclassNames.size() > 0 ? superclassNames.get(0) : new ClassName(Utils.OBJECT_CLASS_NAME);

                AnnotationValue typeArgClassesValue = Utils.findAnnotationValue(desiredSuperclassMirror, "typeArgClasses");
                List<ClassName> superclassTypeArgs = Utils.getClassValuesFromAnnotationValue(typeArgClassesValue);

                AnnotationValue typeArgNames = Utils.findAnnotationValue(desiredSuperclassMirror, "typeArgNames");
                List<String> superclassTypeArgNames = Utils.getStringValuesFromAnnotationValue(typeArgNames);

                AnnotationValue numTypeArgs = Utils.findAnnotationValue(desiredSuperclassMirror, "numTypeArgs");
                int superclassNumTypeArgs = numTypeArgs != null ? ((Integer) numTypeArgs.getValue()).intValue() : 0;

                if (!Utils.isEmpty(superclassTypeArgs)) {
                    desiredSuperclass.setTypeArgs(superclassTypeArgs);
                } else if (!Utils.isEmpty(superclassTypeArgNames)) {
                    desiredSuperclass.setTypeArgs(Utils.map(superclassTypeArgNames, new Utils.MapFunction<String, GenericName>() {
                        @Override
                        public GenericName map(String arg) {
                            return new GenericName(arg, null, null);
                        }
                    }));
                } else if (superclassNumTypeArgs > 0) {
                    List<GenericName> typeArgs = new ArrayList<GenericName>();
                    for (int i = 0; i < superclassNumTypeArgs; i++) {
                        typeArgs.add(new GenericName("S" + Integer.toString(i), null, null));
                    }
                    desiredSuperclass.setTypeArgs(typeArgs);
                }
            }
        } else {
            desiredSuperclass = new ClassName(Utils.OBJECT_CLASS_NAME);
        }

        generatedSuperclass = new ClassName(fqn.toString() + GEN_SUFFIX);
    }

    private void initPreferValues() {
        prefer = new HashMap<String, ClassName>();
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

                    ClassName targetName = Utils.getClassValuesFromAnnotationValue(targetValue).get(0);
                    String method = (String) methodValue.getValue();
                    prefer.put(method, targetName);
                }
            }
        }
    }

    public ClassName getFullyQualifiedGeneratedSuperclassName() {
        return generatedSuperclass;
    }

    public List<TraitElement> getTraitClasses() {
        return traitClasses;
    }

    public ClassName getDesiredSuperclass() {
        return desiredSuperclass;
    }

    public boolean superclassHasTypeArgs() {
        return !Utils.isEmpty(desiredSuperclass.getTypeArgs());
    }

    public Map<String, ClassName> getPreferMap() {
        return prefer;
    }
}
