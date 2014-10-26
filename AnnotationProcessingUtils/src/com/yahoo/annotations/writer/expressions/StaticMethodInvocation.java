package com.yahoo.annotations.writer.expressions;

import java.io.IOException;
import java.util.List;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.writer.JavaFileWriter;

public class StaticMethodInvocation implements Expression {

    private final DeclaredTypeName calledType;
    private final String methodName;
    private final List<String> argumentNames;
    
    public StaticMethodInvocation(DeclaredTypeName calledType, String methodName, List<String> argumentNames) {
        this.calledType = calledType;
        this.methodName = methodName;
        this.argumentNames = argumentNames;
    }
    
    @Override
    public void writeExpression(JavaFileWriter writer) throws IOException {
        if (calledType != null) {
            writer.appendString(writer.shortenNameForStaticReference(calledType)).appendString(".");
        }
        writer.appendString(methodName).writeArgumentNameList(argumentNames);
    }
    
}
