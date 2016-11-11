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
package com.yahoo.javatraits.test.classes;

import com.yahoo.javatraits.annotations.DesiredSuperclass;
import com.yahoo.javatraits.annotations.HasTraits;
import com.yahoo.javatraits.annotations.Prefer;
import com.yahoo.javatraits.test.traits.AnotherTrait;
import com.yahoo.javatraits.test.traits.MathTrait;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
