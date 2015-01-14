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

public @interface DesiredSuperclass {

    /**
     * The class to be used as the superclass
     */
    Class<?> superclass();

    /**
     * Optional argument specifying the number of type
     * arguments the superclass should have. When using
     * this parameter, names for the type arguments will
     * be generated automatically.
     */
    int numTypeArgs() default 0;

    /**
     * Alternative to numTypeArgs that allows you to specify
     * the names of the generics. If values are specified,
     * numTypeArgs will be ignored.
     */
    String[] typeArgNames() default {};

    /**
     * Alternative to typeArgNames that lets you specify
     * the actual type arguments you would like your superclass
     * to have. If values are specified, typeArgNames and numTypeArgs
     * will be ignored.
     */
    Class<?>[] typeArgClasses() default {};

}
