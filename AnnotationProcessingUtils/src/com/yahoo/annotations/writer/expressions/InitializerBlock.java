package com.yahoo.annotations.writer.expressions;

import java.io.IOException;

import com.yahoo.annotations.writer.JavaFileWriter;

class InitializerBlock extends Expression {

    private final Expression blockInternals;
    private final boolean indentStart;
    private final boolean isStatic;
    private final boolean endWithSemicolon;
    private final boolean endWithNewline;
    
    public InitializerBlock(Expression blockInternals, boolean indentStart, 
            boolean isStatic, boolean endWithSemicolon, boolean endWithNewline) {
        this.blockInternals = blockInternals;
        this.indentStart = indentStart;
        this.isStatic = isStatic;
        this.endWithSemicolon = endWithSemicolon;
        this.endWithNewline = endWithNewline;
    }
    
    @Override
    public boolean writeExpression(JavaFileWriter writer) throws IOException {
        writer.beginInitializerBlock(isStatic, indentStart);
        blockInternals.writeExpression(writer);
        writer.finishInitializerBlock(endWithSemicolon, endWithNewline);
        return true;
    }

}
