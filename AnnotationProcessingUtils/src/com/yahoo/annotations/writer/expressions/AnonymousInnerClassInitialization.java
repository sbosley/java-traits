package com.yahoo.annotations.writer.expressions;

import java.io.IOException;
import java.util.List;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.writer.JavaFileWriter;
import com.yahoo.annotations.writer.JavaFileWriter.Scope;

public class AnonymousInnerClassInitialization extends ConstructorInvocation {
    
    public AnonymousInnerClassInitialization(DeclaredTypeName constructorType, List<String> argumentNames) {
        super(constructorType, argumentNames);
    }

    @Override
    public void writeExpression(JavaFileWriter writer) throws IOException {
        super.writeExpression(writer);
        writer.appendString(" {\n")
         .moveToScope(Scope.TYPE_DEFINITION);
    }
}
