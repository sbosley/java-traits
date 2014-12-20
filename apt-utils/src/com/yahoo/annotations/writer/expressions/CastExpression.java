package com.yahoo.annotations.writer.expressions;

import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.writer.JavaFileWriter;

import java.io.IOException;

class CastExpression extends Expression {

    private final TypeName castTo;
    private final Expression toCast;
    
    public CastExpression(TypeName castTo, Expression toCast) {
        this.castTo = castTo;
        this.toCast = toCast;
    }
    
    @Override
    public boolean writeExpression(JavaFileWriter writer) throws IOException {
        writer.appendString("(").appendString(writer.shortenName(castTo, false)).appendString(") ");
        toCast.writeExpression(writer);
        return true;
    }
    
}
