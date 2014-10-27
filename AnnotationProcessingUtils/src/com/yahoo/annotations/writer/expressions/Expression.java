package com.yahoo.annotations.writer.expressions;

import java.io.IOException;
import java.util.List;

import com.yahoo.annotations.utils.Utils;
import com.yahoo.annotations.writer.JavaFileWriter;

public abstract class Expression {

    // returns true if anything was written
    public abstract boolean writeExpression(JavaFileWriter writer) throws IOException;
    
    public Expression callMethod(String methodName, List<?> arguments) {
        return Expressions.callMethod(this, methodName, arguments);
    }
    
    public Expression reference(String fieldName) {
        return Expressions.reference(this, fieldName);
    }
    
    public Expression returnExpr() {
        return Expressions.returnExpr(this);
    }
    
    public static Expression fromString(final String str) {
        return new Expression() {
            @Override
            public boolean writeExpression(JavaFileWriter writer) throws IOException {
                if (!Utils.isEmpty(str)) {
                    writer.appendString(str);
                    return true;
                }
                return false;
            }
        };
    }
    
}
