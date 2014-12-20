package com.yahoo.annotations.writer.expressions;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.writer.JavaFileWriter;

import java.io.IOException;

class StaticFieldReference extends Reference {

    private final DeclaredTypeName typeName;
    
    public StaticFieldReference(DeclaredTypeName typeName, String fieldName) {
        super(fieldName);
        this.typeName = typeName;
    }
    
    @Override
    protected void writeReferencedObject(JavaFileWriter writer) throws IOException {
        writer.appendString(writer.shortenNameForStaticReference(typeName)).appendString(".");
    }
    
}
