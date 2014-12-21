package com.yahoo.aptutils.writer.expressions;

import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;

import java.io.IOException;

abstract class Reference extends Expression {

    private final String fieldName;
    
    public Reference(String fieldName) {
        if (AptUtils.isEmpty(fieldName)) {
            throw new IllegalArgumentException("fieldName can't be null for a Reference expression");
        }
        this.fieldName = fieldName;
    }
    
    @Override
    public boolean writeExpression(JavaFileWriter writer) throws IOException {
        if (writeReferencedObject(writer)) {
            writer.appendString(".");
        }
        writer.appendString(fieldName);
        return true;
    }
    
    protected abstract boolean writeReferencedObject(JavaFileWriter writer) throws IOException;
    
}
