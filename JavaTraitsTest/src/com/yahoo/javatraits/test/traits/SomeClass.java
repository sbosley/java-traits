/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.test.traits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.yahoo.javatraits.annotations.DesiredSuperclass;
import com.yahoo.javatraits.annotations.HasTraits;
import com.yahoo.javatraits.annotations.Prefer;

@HasTraits(traits={MathTrait.class, AnotherTrait.class},
           desiredSuperclass=@DesiredSuperclass(superclass=HashMap.class, typeArgClasses={String.class, Long.class}),
           prefer=@Prefer(target=AnotherTrait.class, method="intToStringV2"))
public class SomeClass<A extends Number, B extends A, C, D> extends SomeClassWithTraits<A, B, C, D> {

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

    @Override
    public Map<String, ArrayList<B[]>>[][] getParametrizedArg() {
        return null;
    }

    @Override
    public <T extends Number & Runnable> void testIntersectionType(T arg) {
        arg.run();
    }

}
