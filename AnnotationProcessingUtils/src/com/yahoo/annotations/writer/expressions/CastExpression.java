package com.yahoo.annotations.writer.expressions;

import java.io.IOException;

import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.writer.JavaFileWriter;

public class CastExpression implements Expression {

    private final TypeName castTo;
    private final Expression toCast;
    
    public CastExpression(TypeName castTo, Expression toCast) {
        this.castTo = castTo;
        this.toCast = toCast;
    }
    
    @Override
    public void writeExpression(JavaFileWriter writer) throws IOException {
        writer.appendString("(").appendString(writer.shortenName(castTo, false)).appendString(") ");
        toCast.writeExpression(writer);
    }
    
}
