/**
 * Copyright 2014 Yahoo Inc.
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation to specify that a class defines a trait.
 * 
 * <br/><br/>
 * 
 * Classes defining traits should be abstract classes with no
 * superclass (i.e. extend only java.lang.Object) and declare
 * no state/instance variable. Traits should also not declare
 * any constructors--the generated code will construct instances
 * for you.
 * 
 * <br/><br/>
 * 
 * Traits implicitly define a Java interface through the methods
 * they declare. The code generator will make these interfaces
 * explicit. A class "MyTrait" annotated with {@literal @}{@link Trait}
 * will cause an interface named "MyTraitInterface" to be generated
 * that declares identical method signatures to those declared
 * in the "MyTrait" abstract class. The code generator will also
 * guarantee that any classes using traits will implement those 
 * interfaces, so you can/should feel free to reference the interfaces
 * elsewhere in your code. (See {@literal @}{@link HasTraits})
 * 
 * @author Sam Bosley <sboz88@gmail.com>
 */
@Target(ElementType.TYPE)
public @interface Trait {}
