package com.yahoo.aptutils.writer.expressions;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;
import com.yahoo.aptutils.writer.JavaFileWriter;

import java.io.IOException;
import java.util.List;

class ConstructorInvocation extends Expression {

    private final DeclaredTypeName constructorType;
    private final List<?> arguments;

    public ConstructorInvocation(DeclaredTypeName constructorType, Object... arguments) {
        this.constructorType = constructorType;
        this.arguments = AptUtils.asList(arguments);
    }
    
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
