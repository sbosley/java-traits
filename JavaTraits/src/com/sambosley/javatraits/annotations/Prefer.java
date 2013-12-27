package com.sambosley.javatraits.annotations;

public @interface Prefer {
    Class<?> target();
    String method();
}
