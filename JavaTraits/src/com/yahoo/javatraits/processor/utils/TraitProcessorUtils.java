package com.yahoo.javatraits.processor.utils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.ErrorType;

public class TraitProcessorUtils {

    public static final String GET_THIS = "getThis";

    public static boolean isGetThis(ExecutableElement exec) {
        return GET_THIS.equals(exec.getSimpleName().toString())
                && exec.getReturnType() instanceof ErrorType // Since it doesn't exist yet
                && exec.getModifiers().contains(Modifier.ABSTRACT)
                && exec.getParameters().size() == 0;
    }

}
