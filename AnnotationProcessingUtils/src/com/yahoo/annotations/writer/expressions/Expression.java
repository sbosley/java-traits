package com.yahoo.annotations.writer.expressions;

import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.writer.JavaFileWriter;

import java.io.IOException;
import java.util.List;

public abstract class Expression {

    // returns true if anything was written
    public abstract boolean writeExpression(JavaFileWriter writer) throws IOException;
    
    public Expression callMethod(String methodName, Object... arguments) {
        return Expressions.callMethodOn(this, methodName, arguments);
    }
    
    public Expression callMethod(String methodName, List<?> arguments) {
        return Expressions.callMethodOn(this, methodName, arguments);
    }
    
    public Expression reference(String fieldName) {
        return Expressions.reference(this, fieldName);
    }
    
    public Expression returnExpr() {
        return Expressions.returnExpr(this);
    }
    
    public Expression cast(TypeName castTo) {
        return Expressions.cast(castTo, this);
    }
    
}
