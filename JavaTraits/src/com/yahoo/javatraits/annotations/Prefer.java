/**
 * Copyright 2014 Yahoo Inc.
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * The {@literal @}{@link Prefer} annotation allows you to manually specify how
 * naming conflicts should be resolved when using multiple traits.
 * 
 * <br/><br/>
 * 
 * The code generator will only use a {@literal @}{@link Prefer} annotation
 * value if it encounters a class that uses two traits with
 * one or more identical method signatures. For the purposes of 
 * comparison, a method signature is considered to be 
 * "{return type} {method name}({comma separated list of argument types})",
 * e.g. "int divideAndRound(double, double)".
 * 
 * <br/><br/>
 * 
 * When two methods do have idential signatures, the {@literal @}{@link HasTraits}
 * annotation will be checked to see if it contains any {@literal @}{@link Prefer}
 * values for either of the target trait classes with a matching
 * {method name}. If so, the implementation of the target trait
 * class will be used; otherwise, the implementation of whichever
 * trait was declared first in the {@literal @}{@link HasTraits} annotation will be used.
 * 
 * @author Sam Bosley <sboz88@gmail.com>
 */
@Target(ElementType.METHOD)
public @interface Prefer {
    /**
     * The target annotation class to prefer
     * for implementing the specified method name.
     */
    Class<?> target();
    
    /**
     * The method name
     */
    String method();
}
