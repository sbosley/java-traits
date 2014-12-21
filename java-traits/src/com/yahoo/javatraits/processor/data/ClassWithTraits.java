/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.data;

import com.yahoo.aptutils.model.CoreTypes;
import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.GenericName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.javatraits.annotations.HasTraits;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassWithTraits extends TypeElementWrapper {

    private static final String GEN_SUFFIX = "WithTraits";

    private List<TraitElement> traitClasses;

    private DeclaredTypeName desiredSuperclass;
    private DeclaredTypeName generatedSuperclass;

    private Map<String, DeclaredTypeName> prefer;

    public ClassWithTraits(TypeElement elem, AptUtils aptUtils) {
        super(elem, aptUtils);
        initTraitClasses();
        initSuperclasses();
        initPreferValues();
    }

    private void initTraitClasses() {
        List<TypeMirror> traitMirrors = aptUtils.getClassMirrorsFromAnnotation(elem, HasTraits.class, "traits");
        traitClasses = AptUtils.map(traitMirrors, new AptUtils.Function<TypeMirror, TraitElement>() {
            @Override
            public TraitElement map(TypeMirror arg) {
                if (!(arg instanceof DeclaredType)) {
                    aptUtils.getMessager().printMessage(Kind.ERROR, "Type mirror " + arg + " for trait argument is not a DeclaredType");
                    return null;
                } else {
                    return new TraitElement((TypeElement) ((DeclaredType) arg).asElement(), aptUtils);
                }
            }
        });
    }

    private void initSuperclasses() {
        AnnotationMirror hasTraits = aptUtils.getAnnotationMirror(elem, HasTraits.class);
        AnnotationValue desiredSuperclassValue = aptUtils.getAnnotationValueFromMirror(hasTraits, "desiredSuperclass");
        if (desiredSuperclassValue != null) {
            Object value = desiredSuperclassValue.getValue();
            if (value instanceof AnnotationMirror) {
                AnnotationMirror desiredSuperclassMirror = (AnnotationMirror) value;
                AnnotationValue superclassValue = aptUtils.getAnnotationValueFromMirror(desiredSuperclassMirror, "superclass");

                List<DeclaredTypeName> superclassNames = aptUtils.getTypeNamesFromAnnotationValue(superclassValue);
                desiredSuperclass = superclassNames.size() > 0 ? superclassNames.get(0) : CoreTypes.JAVA_OBJECT;

                AnnotationValue typeArgClassesValue = aptUtils.getAnnotationValueFromMirror(desiredSuperclassMirror, "typeArgClasses");
                List<DeclaredTypeName> superclassTypeArgs = aptUtils.getTypeNamesFromAnnotationValue(typeArgClassesValue);

                AnnotationValue typeArgNames = aptUtils.getAnnotationValueFromMirror(desiredSuperclassMirror, "typeArgNames");
                List<String> superclassTypeArgNames = aptUtils.getValuesFromAnnotationValue(typeArgNames, String.class);

                AnnotationValue numTypeArgs = aptUtils.getAnnotationValueFromMirror(desiredSuperclassMirror, "numTypeArgs");
                int superclassNumTypeArgs = numTypeArgs != null ? (Integer) numTypeArgs.getValue() : 0;

                if (!AptUtils.isEmpty(superclassTypeArgs)) {
                    desiredSuperclass.setTypeArgs(superclassTypeArgs);
                } else if (!AptUtils.isEmpty(superclassTypeArgNames)) {
                    desiredSuperclass.setTypeArgs(AptUtils.map(superclassTypeArgNames, new AptUtils.Function<String, GenericName>() {
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
            desiredSuperclass = CoreTypes.JAVA_OBJECT;
        }

        generatedSuperclass = new DeclaredTypeName(elementName.toString() + GEN_SUFFIX);
    }

    private void initPreferValues() {
        prefer = new HashMap<String, DeclaredTypeName>();
        AnnotationMirror hasTraits = aptUtils.getAnnotationMirror(elem, HasTraits.class);
        AnnotationValue preferValue = aptUtils.getAnnotationValueFromMirror(hasTraits, "prefer");
        if (preferValue != null && preferValue.getValue() instanceof List) {
            @SuppressWarnings("unchecked")
            List<? extends AnnotationValue> preferList = (List<? extends AnnotationValue>) preferValue.getValue();
            for (AnnotationValue entry : preferList) {
                Object value = entry.getValue();
                if (value instanceof AnnotationMirror) {
                    AnnotationMirror preferMirror = (AnnotationMirror) value;
                    AnnotationValue targetValue = aptUtils.getAnnotationValueFromMirror(preferMirror, "target");
                    AnnotationValue methodValue = aptUtils.getAnnotationValueFromMirror(preferMirror, "method");

                    DeclaredTypeName targetName = aptUtils.getTypeNamesFromAnnotationValue(targetValue).get(0);
                    String method = (String) methodValue.getValue();
                    prefer.put(method, targetName);
                }
            }
        }
    }

    public DeclaredTypeName getGeneratedSuperclassName() {
        return generatedSuperclass;
    }

    public List<TraitElement> getTraitClasses() {
        return traitClasses;
    }

    public DeclaredTypeName getDesiredSuperclass() {
        return desiredSuperclass;
    }

    public boolean superclassHasTypeArgs() {
        return !AptUtils.isEmpty(desiredSuperclass.getTypeArgs());
    }

    public Map<String, DeclaredTypeName> getPreferMap() {
        return prefer;
    }
}
