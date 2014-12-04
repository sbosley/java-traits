package com.yahoo.annotations.writer.expressions;

import com.yahoo.annotations.model.DeclaredTypeName;
import com.yahoo.annotations.model.TypeName;
import com.yahoo.annotations.utils.AptUtils;
import com.yahoo.annotations.writer.JavaFileWriter;

import java.io.IOException;
import java.util.List;

/**
 * Convenience methods for constructing various kinds of {@link com.yahoo.annotations.writer.expressions.Expression}s
 *
 * @see {@link com.yahoo.annotations.writer.expressions.Expression}
 */
public class Expressions {

    /**
     * Converts a string to an Expression object. In other words, whatever string is
     * passed here will be written directly whenever this expression is used
     */
    public static Expression fromString(final String str) {
        return new Expression() {
            @Override
            public boolean writeExpression(JavaFileWriter writer) throws IOException {
                if (!AptUtils.isEmpty(str)) {
                    writer.appendString(str);
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * @param assignTo expression for the left hand side of the assignment
     * @param value expression for the right hand side of the assignment
     *
     * @return an expression representing an assignment
     *
     * E.g. someObject.field = anotherObject.method(arg1, arg2)
     */
    public static Expression assign(Expression assignTo, Expression value) {
        return new AssignmentExpression(assignTo, value);
    }

    /**
     * @param castTo type name to cast the expression to
     * @param value the expression being cast
     *
     * @return an expression representing a cast
     *
     * e.g. (String) someObject
     */
    public static Expression cast(TypeName castTo, Expression value) {
        return new CastExpression(castTo, value);
    }

    /**
     * @param type class name of the constructor to invoke
     * @param arguments constructor arguments
     *
     * @return a constructor invocation expression
     *
     * E.g. new MyObject(arg1, arg2)
     */
    public static Expression callConstructor(DeclaredTypeName type, Object... arguments) {
        return new ConstructorInvocation(type, arguments);
    }

    /**
     * @param type class name of the constructor to invoke
     * @param arguments constructor arguments
     *
     * @return a constructor invocation expression
     *
     * E.g. new MyObject(arg1, arg2)
     */
    public static Expression callConstructor(DeclaredTypeName type, List<?> arguments) {
        return new ConstructorInvocation(type, arguments);
    }

    /**
     * @param body an expression representing the body of this block. This may be a custom expression
     * @param indentStart pass true if the block should be indented, e.g. if it starts on its own line
     * @param isStatic pass true if the block is a static intializer block
     * @param endWithSemicolon pass true if the closing brace of the block should be followed by a semicolon
     * @param endWithNewline pass true if there should be a newline after the closing brace of the block
     *
     * @return an expression for a block. For example, this may be a block for a constant array definition, or
     * an initializer block
     */
    public static Expression block(Expression body, boolean indentStart, boolean isStatic, boolean endWithSemicolon, boolean endWithNewline) {
        return new InitializerBlock(body, indentStart, isStatic, endWithSemicolon, endWithNewline);
    }

    /**
     * @param methodName the name of the method to call
     * @param arguments arguments to the method. May be raw strings or other expressions
     *
     * @return expression to call a method. Note the method is not called on any specific object, so the call
     * is implicitly on "this", i.e. whatever the current context of the Java file is.
     *
     * E.g. someMethod(arg1, arg2)
     */
    public static Expression callMethod(String methodName, Object... arguments) {
        return new MethodInvocation(methodName, arguments);
    }

    /**
     * @param methodName the name of the method to call
     * @param arguments arguments to the method. May be raw strings or other expressions
     *
     * @return expression to call a method. Note the method is not called on any specific object, so the call
     * is implicitly on "this", i.e. whatever the current context of the Java file is.
     *
     * E.g. someMethod(arg1, arg2)
     */
    public static Expression callMethod(String methodName, List<?> arguments) {
        return new MethodInvocation(methodName, arguments);
    }

    /**
     * @param calledObject an {@link com.yahoo.annotations.writer.expressions.Expression} on which to call this
     *                     method. May be an object reference, another method call, or any other expression.
     * @param methodName the name of the method to call
     * @param arguments arguments to the method. May be raw strings or other expressions
     * @return expression to call a method on a specific object.
     * (e.g. expression.someMethod(arg1, arg2) )
     */
    public static Expression callMethodOn(Expression calledObject, String methodName, Object... arguments) {
        return new MethodInvocation(calledObject, methodName, arguments);
    }

    /**
     * @param calledObject an {@link com.yahoo.annotations.writer.expressions.Expression} on which to call this
     *                     method. May be an object reference, another method call, or any other expression.
     * @param methodName the name of the method to call
     * @param arguments method arguments. May be raw strings or other expressions
     *
     * @return expression to call a method on a specific object.
     *
     * E.g. expression.someMethod(arg1, arg2)
     */
    public static Expression callMethodOn(Expression calledObject, String methodName, List<?> arguments) {
        return new MethodInvocation(calledObject, methodName, arguments);
    }

    /**
     * @param calledObject a String naming the object on which to call this method.
     * @param methodName the name of the method to call
     * @param arguments method arguments. May be raw strings or other expressions
     *
     * @return expression to call a method on a specific object.
     *
     * E.g. expression.someMethod(arg1, arg2)
     */
    public static Expression callMethodOn(String calledObject, String methodName, Object... arguments) {
        return reference(calledObject).callMethod(methodName, arguments);
    }

    /**
     * @param calledObject a String naming the object on which to call this method.
     * @param methodName the name of the method to call
     * @param arguments method arguments. May be raw strings or other expressions
     *
     * @return expression to call a method on a specific object.
     *
     * E.g. expression.someMethod(arg1, arg2)
     */
    public static Expression callMethodOn(String calledObject, String methodName, List<?> arguments) {
        return reference(calledObject).callMethod(methodName, arguments);
    }

    /**
     * @param fieldName the field to reference
     *
     * @return an expression representing a field reference. The object containing the field is implicitly "this"
     */
    public static Expression reference(String fieldName) {
        return new ObjectReference(fieldName);
    }

    /**
     * @param referencedObject an expression representing the object containing the field to be referenced
     * @param fieldName the name of the field to be referenced
     *
     * @return an expression representing the field reference
     *
     * E.g. if the referenced object is a method that returns an object with public fields:
     * myMethod().publicField
     */
    public static Expression reference(Expression referencedObject, String fieldName) {
        return new ObjectReference(referencedObject, fieldName);
    }

    /**
     * @param referencedObject an string representing the name of the object containing the field to be referenced
     * @param fieldName the name of the field to be referenced
     *
     * @return an expression representing the field reference
     *
     * E.g. referencedObject.fieldName
     */
    public static Expression reference(String referencedObject, String fieldName) {
        return new ObjectReference(referencedObject, fieldName);
    }

    /**
     * @param toReturn expression representing the value to return
     *
     * @return an expression representing a return statement
     */
    public static Expression returnExpr(Expression toReturn) {
        return new ReturnExpression(toReturn);
    }

    /**
     * @param typeName the name of the class being referenced
     * @param fieldName the name of the static field to reference
     *
     * @return an expression representing a static reference
     *
     * E.g. MyClass.SOME_CONSTANT
     */
    public static Expression staticReference(DeclaredTypeName typeName, String fieldName) {
        return new StaticFieldReference(typeName, fieldName);
    }

    /**
     * @param typeName class name
     *
     * @return an expression representing a reference to the class field
     *
     * E.g. MyClass.class
     */
    public static Expression classObject(DeclaredTypeName typeName) {
        return staticReference(typeName, "class");
    }

    /**
     * @param type class to call the static method on
     * @param methodName static method name
     * @param arguments method arguments. May be raw strings or other expressions
     *
     * @return an expression representing a static method call
     *
     * E.g. MyClass.staticMethod(arg1, arg2)l
     */
    public static Expression staticMethod(DeclaredTypeName type, String methodName, Object... arguments) {
        return new StaticMethodInvocation(type, methodName, arguments);
    }

    /**
     * @param type class to call the static method on
     * @param methodName static method name
     * @param arguments method arguments. May be raw strings or other expressions
     *
     * @return an expression representing a static method call
     *
     * E.g. MyClass.staticMethod(arg1, arg2)l
     */
    public static Expression staticMethod(DeclaredTypeName type, String methodName, List<?> arguments) {
        return new StaticMethodInvocation(type, methodName, arguments);
    }
}
