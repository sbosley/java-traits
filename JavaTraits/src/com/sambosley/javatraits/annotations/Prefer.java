package com.sambosley.javatraits.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
public @interface Prefer {
    Class<?> target();
    String method();
}
