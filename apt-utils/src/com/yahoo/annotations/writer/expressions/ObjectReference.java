package com.yahoo.annotations.writer.expressions;

import com.yahoo.annotations.utils.AptUtils;
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
    protected boolean writeReferencedObject(JavaFileWriter writer) throws IOException {
        if (!AptUtils.isEmpty(referencedObject)) {
            writer.appendString(referencedObject);
            return true;
        } else if (referencedExpression != null) {
            referencedExpression.writeExpression(writer);
            return true;
        }
        return false;
    }

}
