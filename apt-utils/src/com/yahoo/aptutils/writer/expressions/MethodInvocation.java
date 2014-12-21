package com.yahoo.aptutils.writer.expressions;

import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;

import java.io.IOException;
import java.util.List;

class MethodInvocation extends Expression {

    private Expression calledObject;
    private final String methodName;
    private final List<?> arguments;
    
    public MethodInvocation(String methodName, List<?> arguments) {
        this.methodName = methodName;
        this.arguments = arguments;
    }
    
    public MethodInvocation(String methodName, Object... arguments) {
        this(methodName, AptUtils.asList(arguments));
    }
    
    public MethodInvocation(Expression calledObject, String methodName, Object... arguments) {
        this(methodName, arguments);
        this.calledObject = calledObject;
    }
    
    public MethodInvocation(Expression calledObject, String methodName, List<?> arguments) {
        this(methodName, arguments);
        this.calledObject = calledObject;
    }
    
    @Override
    public boolean writeExpression(JavaFileWriter writer) throws IOException {
        if (calledObject != null && calledObject.writeExpression(writer)) {
            writer.appendString(".");
        }
        writer.appendString(methodName).writeArgumentNameList(arguments);
        return true;
    }
    
}
