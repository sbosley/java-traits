/**
 * Copyright 2014 Yahoo Inc.
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.test;

import com.yahoo.javatraits.annotations.Trait;

@Trait
public abstract class MathTrait<A extends Number, B extends A> {
    public int add(int arg1, int arg2) {
        return arg1 + arg2;
    }

    public int subtract(int arg1, int arg2) {
        return arg1 - arg2;
    }

    public abstract int someWeirdOp(int arg1, int arg2) throws Exception;

    public int multiplyByTwoAndThenSomeWeirdOp(int arg1, int arg2) throws Exception {
        arg1 *= 2;
        arg2 *= 2;
        return someWeirdOp(arg1, arg2);
    }

    public int sum(int... args) {
        int sum = 0;
        if (args != null) {
            for (int i : args)
                sum += i;
        }
        return sum;
    }
    
    public abstract B transform(A a);
    
    public <D extends Number> int numberToInt(D number) {
        return number.intValue();
    }
    
    public String duplicateMethod(int someArg) {
        return Integer.toHexString(someArg);
    }
}
