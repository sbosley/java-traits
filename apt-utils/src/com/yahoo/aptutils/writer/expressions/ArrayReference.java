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

import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;

import java.io.IOException;

class ArrayReference extends Expression {

    private Expression referencedExpression;
    private String referencedObject;
    private int index;

    public ArrayReference(String array, int index) {
        this.referencedObject = array;
        this.index = index;
    }

    public ArrayReference(Expression array, int index) {
        this.referencedExpression = array;
        this.index = index;
    }

    @Override
    public boolean writeExpression(JavaFileWriter writer) throws IOException {
        if (!AptUtils.isEmpty(referencedObject)) {
            writer.appendString(referencedObject);
        } else {
            writer.appendExpression(referencedExpression);
        }
        writer.appendString("[" + index + "]");
        return true;
    }
}
