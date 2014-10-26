package com.yahoo.annotations.writer.expressions;

import java.io.IOException;
import java.util.List;

import com.yahoo.annotations.utils.Utils;
import com.yahoo.annotations.writer.JavaFileWriter;

public class MethodInvocation implements Expression {

    private final String calledObject;
    private final String methodName;
    private final List<String> argumentNames;
    
    public MethodInvocation(String calledObject, String methodName, List<String> argumentNames) {
        this.calledObject = calledObject;
        this.methodName = methodName;
        this.argumentNames = argumentNames;
    }
    
    @Override
    public void writeExpression(JavaFileWriter writer) throws IOException {
        if (!Utils.isEmpty(calledObject)) {
            writer.appendString(calledObject).appendString(".");
        }
        writer.appendString(methodName).writeArgumentNameList(argumentNames);
    }
    
}
