package com.yahoo.annotations.writer.expressions;

import java.io.IOException;

import com.yahoo.annotations.writer.JavaFileWriter;

public class InitializerBlock implements Expression {

    private final Expression blockInternals;
    
    public InitializerBlock(Expression blockInternals) {
        this.blockInternals = blockInternals;
    }
    
    @Override
    public void writeExpression(JavaFileWriter writer) throws IOException {
        writer.beginInitializerBlock(false, false);
        blockInternals.writeExpression(writer);
        writer.finishInitializerBlock();
    }

}
