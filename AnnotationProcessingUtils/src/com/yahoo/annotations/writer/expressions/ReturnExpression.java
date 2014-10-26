package com.yahoo.annotations.writer.expressions;

import java.io.IOException;

import com.yahoo.annotations.writer.JavaFileWriter;

public class ReturnExpression implements Expression {

    private final Expression toReturn;
    
    public ReturnExpression(Expression toReturn) {
        this.toReturn = toReturn;
    }
    
    @Override
    public void writeExpression(JavaFileWriter writer) throws IOException {
        writer.appendString("return ").appendExpression(toReturn);
    }
    
}
