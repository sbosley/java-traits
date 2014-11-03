package com.yahoo.javatraits.processor.utils;

import com.yahoo.annotations.utils.Utils;
import com.yahoo.javatraits.processor.data.TraitElement;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

public class TraitProcessorUtils extends Utils {

    public TraitProcessorUtils(Messager messager, Types types) {
        super(messager, types);
    }

    public static final String GET_THIS = "getThis";

    public boolean isGetThis(TraitElement element, ExecutableElement exec) {
        return GET_THIS.equals(exec.getSimpleName().toString())
                && checkReturnType(element, exec)
                && exec.getModifiers().contains(Modifier.ABSTRACT)
                && exec.getParameters().size() == 0;
    }
    
    private boolean checkReturnType(TraitElement element, ExecutableElement exec) {
        TypeMirror returnType = exec.getReturnType();
        if (returnType instanceof ErrorType) { // It may not exist yet
            return true;
        } else {
            return element.getGeneratedInterfaceName().equals(getTypeNameFromTypeMirror(returnType, null));
        }
    }
}
