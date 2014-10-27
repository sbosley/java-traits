package com.yahoo.annotations.writer.expressions;

import java.io.IOException;
import java.util.List;

import com.yahoo.annotations.utils.Utils;
import com.yahoo.annotations.writer.JavaFileWriter;

class MethodInvocation extends Expression {

    private String calledObjectString;
    private Expression calledObject;
    private final String methodName;
    private final List<?> arguments;
    
    public MethodInvocation(String methodName, List<?> arguments) {
        this.methodName = methodName;
        this.arguments = arguments;
    }
    
    public MethodInvocation(String methodName, Object... arguments) {
        this(methodName, Utils.asList(arguments));
    }
    
    public MethodInvocation(String calledObjectString, String methodName, Object... arguments) {
        this(methodName, arguments);
        this.calledObjectString = calledObjectString;
    }
    
    public MethodInvocation(Expression calledObject, String methodName, Object... arguments) {
        this(methodName, arguments);
        this.calledObject = calledObject;
    }
    
    public MethodInvocation(String calledObjectString, String methodName, List<?> arguments) {
        this(methodName, arguments);
        this.calledObjectString = calledObjectString;
    }
    
    public MethodInvocation(Expression calledObject, String methodName, List<?> arguments) {
        this(methodName, arguments);
        this.calledObject = calledObject;
    }
    
    @Override
    public boolean writeExpression(JavaFileWriter writer) throws IOException {
        if (!Utils.isEmpty(calledObjectString)) {
            writer.appendString(calledObjectString).appendString(".");
        } else if (calledObject != null && calledObject.writeExpression(writer)) {
            writer.appendString(".");
        }
        writer.appendString(methodName).writeArgumentNameList(arguments);
        return true;
    }
    
}
