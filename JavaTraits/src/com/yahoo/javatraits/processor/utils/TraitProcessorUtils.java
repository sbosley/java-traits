package com.yahoo.javatraits.processor.utils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeMirror;

import com.yahoo.annotations.utils.Utils;
import com.yahoo.javatraits.processor.data.TraitElement;

public class TraitProcessorUtils {

    public static final String GET_THIS = "getThis";

    public static boolean isGetThis(Utils utils, TraitElement element, ExecutableElement exec) {
        return GET_THIS.equals(exec.getSimpleName().toString())
                && checkReturnType(utils, element, exec)
                && exec.getModifiers().contains(Modifier.ABSTRACT)
                && exec.getParameters().size() == 0;
    }
    
    private static boolean checkReturnType(Utils utils, TraitElement element, ExecutableElement exec) {
        TypeMirror returnType = exec.getReturnType();
        if (returnType instanceof ErrorType) { // It may not exist yet
            return true;
        } else {
            return element.getInterfaceName().equals(utils.getTypeNameFromTypeMirror(returnType, null));
        }
    }

}
