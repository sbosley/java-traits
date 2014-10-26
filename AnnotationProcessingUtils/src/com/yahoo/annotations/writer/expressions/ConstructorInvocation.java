package com.yahoo.annotations.writer.expressions;

import java.io.IOException;
import java.util.List;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.writer.JavaFileWriter;

public class ConstructorInvocation implements Expression {

    private final DeclaredTypeName constructorType;
    private final List<String> argumentNames;

    public ConstructorInvocation(DeclaredTypeName constructorType, List<String> argumentNames) {
        this.constructorType = constructorType;
        this.argumentNames = argumentNames;
    }
    
    @Override
    public void writeExpression(JavaFileWriter writer) throws IOException {
        writer.appendString("new ").appendString(writer.shortenName(constructorType, false));
        writer.writeArgumentNameList(argumentNames);
    }
    
}
