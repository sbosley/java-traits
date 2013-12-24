package com.sambosley.javatraits.test;

import com.sambosley.javatraits.annotations.HasTraits;

@HasTraits(traits={MyTrait.class, AnotherTrait.class})
public class ClassWithTraits<A extends Number, B extends A, C, D> extends ClassWithTraitsGen<A, B, C, D> {

    @Override
    public int someWeirdOp(int arg1, int arg2) {
        return 0;
    }
    
    @Override
    public B transform(A a) {
        return null;
    }
    
    @Override
    public C[] copyANTimes(C a, int n) {
        return null;
    }

    @Override
    public D[] copyBNTimes(D a, int n) {
        return null;
    }
}
