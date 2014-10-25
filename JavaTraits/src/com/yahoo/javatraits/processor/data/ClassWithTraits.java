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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import com.yahoo.annotations.model.ClassName;
import com.yahoo.annotations.model.GenericName;
import com.yahoo.annotations.utils.Utils;
import com.yahoo.javatraits.annotations.HasTraits;

public class ClassWithTraits extends TypeElementWrapper {

    private static final String GEN_SUFFIX = "WithTraits";

    private List<TraitElement> traitClasses;

    private ClassName desiredSuperclass;

    private ClassName generatedSuperclass;
    private Map<String, ClassName> prefer;

    public ClassWithTraits(TypeElement elem, Utils utils) {
        super(elem, utils);
        initTraitClasses();
        initSuperclasses();
        initPreferValues();
    }

    private void initTraitClasses() {
        List<TypeMirror> traitMirrors = utils.getClassMirrorsFromAnnotation(elem, HasTraits.class, "traits");
        traitClasses = Utils.map(traitMirrors, new Utils.Mapper<TypeMirror, TraitElement>() {
            @Override
            public TraitElement map(TypeMirror arg) {
                if (!(arg instanceof DeclaredType)) {
                    utils.getMessager().printMessage(Kind.ERROR, "Type mirror " + arg + " for trait argument is not a DeclaredType");
                    return null;
                } else {
                    return new TraitElement((TypeElement) ((DeclaredType) arg).asElement(), utils);
                }
            }
        });
    }

    private void initSuperclasses() {
        AnnotationMirror hasTraits = utils.getAnnotationMirror(elem, HasTraits.class);
        AnnotationValue desiredSuperclassValue = utils.getAnnotationValueFromMirror(hasTraits, "desiredSuperclass");
        if (desiredSuperclassValue != null) {
            Object value = desiredSuperclassValue.getValue();
            if (value instanceof AnnotationMirror) {
                AnnotationMirror desiredSuperclassMirror = (AnnotationMirror) value;
                AnnotationValue superclassValue = utils.getAnnotationValueFromMirror(desiredSuperclassMirror, "superclass");

                List<ClassName> superclassNames = utils.getClassValuesFromAnnotationValue(superclassValue);
                desiredSuperclass = superclassNames.size() > 0 ? superclassNames.get(0) : new ClassName(Utils.OBJECT_CLASS_NAME);

                AnnotationValue typeArgClassesValue = utils.getAnnotationValueFromMirror(desiredSuperclassMirror, "typeArgClasses");
                List<ClassName> superclassTypeArgs = utils.getClassValuesFromAnnotationValue(typeArgClassesValue);

                AnnotationValue typeArgNames = utils.getAnnotationValueFromMirror(desiredSuperclassMirror, "typeArgNames");
                List<String> superclassTypeArgNames = utils.getStringValuesFromAnnotationValue(typeArgNames);

                AnnotationValue numTypeArgs = utils.getAnnotationValueFromMirror(desiredSuperclassMirror, "numTypeArgs");
                int superclassNumTypeArgs = numTypeArgs != null ? ((Integer) numTypeArgs.getValue()).intValue() : 0;

                if (!Utils.isEmpty(superclassTypeArgs)) {
                    desiredSuperclass.setTypeArgs(superclassTypeArgs);
                } else if (!Utils.isEmpty(superclassTypeArgNames)) {
                    desiredSuperclass.setTypeArgs(Utils.map(superclassTypeArgNames, new Utils.Mapper<String, GenericName>() {
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
        AnnotationMirror hasTraits = utils.getAnnotationMirror(elem, HasTraits.class);
        AnnotationValue preferValue = utils.getAnnotationValueFromMirror(hasTraits, "prefer");
        if (preferValue != null && preferValue.getValue() instanceof List) {
            @SuppressWarnings("unchecked")
            List<? extends AnnotationValue> preferList = (List<? extends AnnotationValue>) preferValue.getValue();
            for (AnnotationValue entry : preferList) {
                Object value = entry.getValue();
                if (value instanceof AnnotationMirror) {
                    AnnotationMirror preferMirror = (AnnotationMirror) value;
                    AnnotationValue targetValue = utils.getAnnotationValueFromMirror(preferMirror, "target");
                    AnnotationValue methodValue = utils.getAnnotationValueFromMirror(preferMirror, "method");

                    ClassName targetName = utils.getClassValuesFromAnnotationValue(targetValue).get(0);
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
