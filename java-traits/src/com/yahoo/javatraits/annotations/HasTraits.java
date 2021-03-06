/*
 * Copyright 2014 Yahoo Inc.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yahoo.javatraits.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * The {@link @HasTraits} annotation allows you to declare that
 * classes implement a set of traits.
 *
 * <br/><br/>
 *
 * Classes that use {@literal @}{@link HasTraits} cause several supporting classes
 * to be generated at compile time. One such class will be the intended
 * superclass for the annotated class. The generated superclass will have
 * the same name as the annotated class with the suffix "Gen". If you want
 * your class to actually have the traits specified, you should make sure
 * that it extends the intended generated superclass. For example, a class
 * "MyClass" that uses the {@literal @}{@link HasTraits} annotation should extend "MyClassGen".
 * <br/>
 * Example:
 * <pre>
 * {@literal @}HasTraits(traits={MyTrait.class})
 * public class MyClass extends MyClassGen {
 *    ...
 * }
 * </pre>
 *
 * The code generator will guarantee that the generated superclasses
 * implement all the interfaces implicitly defined by the specified
 * {@literal @}{@link Trait} classes.
 *
 * @author Sam Bosley
 */
@Target(ElementType.TYPE)
public @interface HasTraits {
    /**
     * A list of trait classes (i.e. classes annotated with {@literal @}{@link Trait})
     * that the annotated class uses.
     */
    Class<?>[] traits();

    /**
     * An annotation describing the desired superclass for this class. Classes annotated with
     * {@literal @}{@link HasTraits} will have to subclass a generated abstract class, but
     * if you want the generated subclass to have a parent class that is not
     * {@literal @}{@link Object} you can specify it here using the
     * {@ literal @}{@link DesiredSuperclass} annotation;
     */
    DesiredSuperclass desiredSuperclass() default @DesiredSuperclass(superclass=Object.class);

    /**
     * Optional list of {@literal @}{@link Prefer} annotations that allow manual control
     * when resolving method naming conflicts.
     */
    Prefer[] prefer() default {};
}
