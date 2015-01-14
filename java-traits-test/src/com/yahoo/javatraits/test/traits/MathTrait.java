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
package com.yahoo.javatraits.test.traits;

import com.yahoo.javatraits.annotations.Trait;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Trait
public abstract class MathTrait<A extends Number, B extends A> {

    public static final double PI = Math.PI;

    public abstract Map<String, ArrayList<B[]>>[][] getParametrizedArg();

    public abstract IMathTrait<A, B> getThis();

    public void doSomeListThing(List<? super ArrayList<? extends CharSequence>>[][]... strings) {
        //
    }
    
    public abstract <T extends Number & Runnable> void testIntersectionType(T arg);

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

    public String intToStringV1(int someArg) {
        return Integer.toHexString(someArg);
    }
    
    public String intToStringV2(int someArg) {
        return Integer.toHexString(someArg);
    }
}
