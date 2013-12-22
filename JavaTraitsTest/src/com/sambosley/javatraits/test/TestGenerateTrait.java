package com.sambosley.javatraits.test;

import com.sambosley.javatraits.annotations.Trait;

@Trait
public abstract class TestGenerateTrait {
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
}
