package com.yahoo.aptutils.writer.expressions;

import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.writer.JavaFileWriter;

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
