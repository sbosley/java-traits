package com.yahoo.annotations.writer.expressions;

import com.yahoo.annotations.utils.AptUtils;
import com.yahoo.annotations.writer.JavaFileWriter;

import java.io.IOException;

abstract class Reference extends Expression {

    private final String fieldName;
    
    public Reference(String fieldName) {
        if (AptUtils.isEmpty(fieldName)) {
            throw new IllegalArgumentException("fieldName can't be numm for a FieldReference expression");
        }
        this.fieldName = fieldName;
    }
    
    @Override
    public boolean writeExpression(JavaFileWriter writer) throws IOException {
        writeReferencedObject(writer);
        writer.appendString(fieldName);
        return true;
    }
    
    protected abstract void writeReferencedObject(JavaFileWriter writer) throws IOException;
    
}
