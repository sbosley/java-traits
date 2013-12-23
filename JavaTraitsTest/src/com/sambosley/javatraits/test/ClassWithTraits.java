package com.sambosley.javatraits.test;

import com.sambosley.javatraits.annotations.HasTraits;

@HasTraits(traits={MyTrait.class})
public class ClassWithTraits extends ClassWithTraitsGen<Integer, String> {

    @Override
    public int someWeirdOp(int arg1, int arg2) {
        return 0;
    }
    
    @Override
    public String transform(Integer a) {
        return Integer.toString(a);
    }

}
