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
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;

import java.io.IOException;
import java.util.List;

class StaticMethodInvocation extends Expression {

    private final DeclaredTypeName calledType;
    private final String methodName;
    private final List<?> arguments;
    
    public StaticMethodInvocation(DeclaredTypeName calledType, String methodName, Object... arguments) {
        this(calledType, methodName, AptUtils.asList(arguments));
    }
    
    public StaticMethodInvocation(DeclaredTypeName calledType, String methodName, List<?> arguments) {
        this.calledType = calledType;
        this.methodName = methodName;
        this.arguments = arguments;
    }
    
    @Override
    public boolean writeExpression(JavaFileWriter writer) throws IOException {
        if (calledType != null) {
            writer.appendString(writer.shortenNameForStaticReference(calledType)).appendString(".");
        }
        writer.appendString(methodName).writeArgumentNameList(arguments);
        return true;
    }
    
}
