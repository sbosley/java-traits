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
package com.yahoo.aptutils.writer.expressions;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.writer.JavaFileWriter;

import java.io.IOException;

class ArrayAllocation extends Expression {

    private DeclaredTypeName arrayBaseType;
    private int arrayDepth;
    private int[] sizes;

    ArrayAllocation(DeclaredTypeName arrayBaseType, int arrayDepth, int... sizes) {
        if (arrayDepth != sizes.length) {
            throw new IllegalArgumentException("Must specify as many sizes as the array is deep");
        }
        if (arrayDepth < 1) {
            throw new IllegalArgumentException("Array depth must be at least 1");
        }
        this.arrayBaseType = arrayBaseType;
        this.arrayDepth = arrayDepth;
        this.sizes = sizes;
    }

    @Override
    public boolean writeExpression(JavaFileWriter writer) throws IOException {
        writer.appendString("new ").appendString(writer.shortenName(arrayBaseType, false));
        for (int size : sizes) {
            writer.appendString("[" + size + "]");
        }
        return true;
    }
}
