package com.yahoo.annotations.writer.expressions;

import com.yahoo.annotations.utils.Utils;
import com.yahoo.annotations.writer.JavaFileWriter;

import java.io.IOException;

class ObjectReference extends Reference {

    private String referencedObject;
    private Expression referencedExpression;
    
    public ObjectReference(String fieldName) {
        super(fieldName);
    }
    
    public ObjectReference(String referencedObject, String fieldName) {
        this(fieldName);
        this.referencedObject = referencedObject;
    }
    
    public ObjectReference(Expression referencedExpression, String fieldName) {
        this(fieldName);
        this.referencedExpression = referencedExpression;
    }
    
    @Override
    protected void writeReferencedObject(JavaFileWriter writer) throws IOException {
        if (!Utils.isEmpty(referencedObject)) {
            writer.appendString(referencedObject).appendString(".");
        } else if (referencedExpression != null) {
            referencedExpression.writeExpression(writer);
        }
    }

}
