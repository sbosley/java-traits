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

class ObjectReference extends Reference {

    private String referencedObject;
    private Expression referencedExpression;
    
    ObjectReference(String fieldName) {
        super(fieldName);
    }
    
    ObjectReference(String referencedObject, String fieldName) {
        this(fieldName);
        this.referencedObject = referencedObject;
    }
    
    ObjectReference(Expression referencedExpression, String fieldName) {
        this(fieldName);
        this.referencedExpression = referencedExpression;
    }
    
    @Override
    protected boolean writeReferencedObject(JavaFileWriter writer) throws IOException {
        if (!AptUtils.isEmpty(referencedObject)) {
            writer.appendString(referencedObject);
            return true;
        } else if (referencedExpression != null) {
            referencedExpression.writeExpression(writer);
            return true;
        }
        return false;
    }

}
