package com.yahoo.annotations.writer.expressions;

import java.io.IOException;

import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.writer.JavaFileWriter;

class StaticFieldReference extends Reference {

    private final TypeName typeName;
    
    public StaticFieldReference(TypeName typeName, String fieldName) {
        super(fieldName);
        this.typeName = typeName;
    }
    
    @Override
    protected void writeReferencedObject(JavaFileWriter writer) throws IOException {
        writer.appendString(writer.shortenNameForStaticReference(typeName)).appendString(".");
    }
    
}
