package com.yahoo.annotations.writer.expressions;

import com.yahoo.annotations.writer.JavaFileWriter;

import java.io.IOException;

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
