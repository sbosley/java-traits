/**
 * Copyright 2014 Yahoo Inc.
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.test.traits;

import com.yahoo.javatraits.annotations.Trait;

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
    
    public String intToStringV1(int someArg) {
        return Integer.toBinaryString(someArg);
    }
    
    public String intToStringV2(int someArg) {
        return Integer.toBinaryString(someArg);
    }
}
