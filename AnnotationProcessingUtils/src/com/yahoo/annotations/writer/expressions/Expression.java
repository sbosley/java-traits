package com.yahoo.annotations.writer.expressions;

import java.io.IOException;

import com.yahoo.annotations.writer.JavaFileWriter;

public interface Expression {

    public void writeExpression(JavaFileWriter writer) throws IOException;
    
}
