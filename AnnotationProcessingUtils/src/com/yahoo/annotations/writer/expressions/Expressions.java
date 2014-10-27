package com.yahoo.annotations.writer.expressions;

import java.io.IOException;
import java.util.List;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.utils.Utils;
import com.yahoo.annotations.writer.JavaFileWriter;

public class Expressions {
    
    public static Expression fromString(final String str) {
        return new Expression() {
            @Override
            public boolean writeExpression(JavaFileWriter writer) throws IOException {
                if (!Utils.isEmpty(str)) {
                    writer.appendString(str);
                    return true;
                }
                return false;
            }
        };
    }
    
    public static Expression assign(Expression assignTo, Expression value) {
        return new AssignmentExpression(assignTo, value);
    }
    
    public static Expression cast(TypeName castTo, Expression value) {
        return new CastExpression(castTo, value);
    }
    
    public static Expression callConstructor(DeclaredTypeName type, Object... arguments) {
        return new ConstructorInvocation(type, arguments);
    }
    
    public static Expression callConstructor(DeclaredTypeName type, List<?> arguments) {
        return new ConstructorInvocation(type, arguments);
    }
    
    public static Expression block(Expression body, boolean indentStart, boolean isStatic, boolean endWithSemicolon, boolean endWithNewline) {
        return new InitializerBlock(body, indentStart, isStatic, endWithSemicolon, endWithNewline);
    }
    
    public static Expression callMethod(String methodName, Object... arguments) {
        return new MethodInvocation(methodName, arguments);
    }
    
    public static Expression callMethod(String methodName, List<?> arguments) {
        return new MethodInvocation(methodName, arguments);
    }
    
    public static Expression callMethod(Expression calledObject, String methodName, Object... arguments) {
        return new MethodInvocation(calledObject, methodName, arguments);
    }
    
    public static Expression callMethod(Expression calledObject, String methodName, List<?> arguments) {
        return new MethodInvocation(calledObject, methodName, arguments);
    }
    
    public static Expression callMethod(String calledObject, String methodName, Object... arguments) {
        return new MethodInvocation(calledObject, methodName, arguments);
    }
    
    public static Expression callMethod(String calledObject, String methodName, List<?> arguments) {
        return new MethodInvocation(calledObject, methodName, arguments);
    }
    
    public static Expression reference(String fieldName) {
        return new ObjectReference(fieldName);
    }
    
    public static Expression reference(Expression referencedObject, String fieldName) {
        return new ObjectReference(referencedObject, fieldName);
    }

    public static Expression reference(String referencedObject, String fieldName) {
        return new ObjectReference(referencedObject, fieldName);
    }
    
    public static Expression returnExpr(Expression toReturn) {
        return new ReturnExpression(toReturn);
    }
    
    public static Expression staticReference(TypeName typeName, String fieldName) {
        return new StaticFieldReference(typeName, fieldName);
    }
    
    public static Expression classObject(TypeName typeName) {
        return staticReference(typeName, "class");
    }
    
    public static Expression staticMethod(DeclaredTypeName type, String methodName, Object... arguments) {
        return new StaticMethodInvocation(type, methodName, arguments);
    }
    
    public static Expression staticMethod(DeclaredTypeName type, String methodName, List<?> arguments) {
        return new StaticMethodInvocation(type, methodName, arguments);
    }
}
