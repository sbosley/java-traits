package com.sambosley.javatraits.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface HasTraits {
    Class<?>[] traits();
    Class<?> desiredSuperclass() default Object.class;
    Prefer[] prefer() default {};
}
