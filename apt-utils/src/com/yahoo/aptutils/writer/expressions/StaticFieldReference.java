package com.yahoo.aptutils.writer.expressions;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.writer.JavaFileWriter;

import java.io.IOException;

class StaticFieldReference extends Reference {

    private final DeclaredTypeName typeName;
    
    public StaticFieldReference(DeclaredTypeName typeName, String fieldName) {
        super(fieldName);
        this.typeName = typeName;
    }
    
    @Override
    protected boolean writeReferencedObject(JavaFileWriter writer) throws IOException {
        writer.appendString(writer.shortenNameForStaticReference(typeName));
        return true;
    }
    
}
