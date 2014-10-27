package com.yahoo.annotations.writer.expressions;

import java.io.IOException;
import java.util.List;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.writer.JavaFileWriter;

class ConstructorInvocation extends Expression {

    private final DeclaredTypeName constructorType;
    private final List<?> arguments;

    public ConstructorInvocation(DeclaredTypeName constructorType, List<?> arguments) {
        this.constructorType = constructorType;
        this.arguments = arguments;
    }
    
    @Override
    public boolean writeExpression(JavaFileWriter writer) throws IOException {
        writer.appendString("new ").appendString(writer.shortenName(constructorType, false));
        writer.writeArgumentNameList(arguments);
        return true;
    }
    
}
