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

@Trait
public abstract class AnotherTrait<A, B> {

    private int testVariable;

    public int getTestVariable() {
        return testVariable;
    }

    public void setTestVariable(int testVariable) {
        this.testVariable = testVariable;
    }

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

    @Deprecated
    public String intToStringV2(int someArg) {
        return Integer.toBinaryString(someArg);
    }
}
