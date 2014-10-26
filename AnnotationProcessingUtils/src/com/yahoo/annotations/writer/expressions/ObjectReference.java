package com.yahoo.annotations.writer.expressions;

import java.io.IOException;

import com.yahoo.annotations.utils.Utils;
import com.yahoo.annotations.writer.JavaFileWriter;

public class ObjectReference extends Reference {

    private final String referencedObject;
    
    public ObjectReference(String referencedObject, String fieldName) {
        super(fieldName);
        this.referencedObject = referencedObject;
    }
    
    @Override
    protected void writeReferencedObject(JavaFileWriter writer) throws IOException {
        if (!Utils.isEmpty(referencedObject)) {
            writer.appendString(referencedObject).appendString(".");
        }
    }

}
