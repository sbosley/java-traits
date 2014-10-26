package com.yahoo.annotations.writer.expressions;

import java.io.IOException;

import com.yahoo.annotations.writer.JavaFileWriter;

public class AssignmentExpression implements Expression {

    private Reference assignTo;
    private Expression assignFrom;
    
    public AssignmentExpression(Reference assignTo, Expression assignFrom) {
        this.assignTo = assignTo;
        this.assignFrom = assignFrom;
    }
    
    @Override
    public void writeExpression(JavaFileWriter writer) throws IOException {
        assignTo.writeExpression(writer);
        writer.appendString(" = ");
        assignFrom.writeExpression(writer);
    }
    
}
