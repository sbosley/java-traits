package com.yahoo.annotations.writer.expressions;

import java.io.IOException;

import com.yahoo.annotations.writer.JavaFileWriter;

class ReturnExpression extends Expression {

    private final Expression toReturn;
    
    public ReturnExpression(Expression toReturn) {
        this.toReturn = toReturn;
    }
    
    @Override
    public boolean writeExpression(JavaFileWriter writer) throws IOException {
        writer.appendString("return ").appendExpression(toReturn);
        return true;
    }
    
}
