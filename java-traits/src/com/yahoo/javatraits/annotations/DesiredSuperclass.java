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
