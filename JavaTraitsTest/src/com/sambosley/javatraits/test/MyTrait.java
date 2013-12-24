package com.sambosley.javatraits.test;

import com.sambosley.javatraits.annotations.Trait;

@Trait
public abstract class MyTrait<A extends Number, B extends A> {
    public int add(int arg1, int arg2) {
        return arg1 + arg2;
    }

    public int subtract(int arg1, int arg2) {
        return arg1 - arg2;
    }

    public abstract int someWeirdOp(int arg1, int arg2);

    public int multiplyByTwoAndThenSomeWeirdOp(int arg1, int arg2) {
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
}
