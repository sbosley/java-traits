package com.sambosley.javatraits.test;

import com.sambosley.javatraits.annotations.Trait;

@Trait
public abstract class AnotherTrait<A, B> {
    
    public abstract A[] copyANTimes(A a, int n);
    
    public abstract B[] copyBNTimes(B b, int n);
    
    public int countAs(A... args) {
        if (args != null)
            return args.length;
        return 0;
    }
    
    public int countBs(B... args) {
        if (args != null)
            return args.length;
        return 0;
    }
    
    public int subtract(int arg1, int arg2) {
        return arg2 - arg1;
    }
}
