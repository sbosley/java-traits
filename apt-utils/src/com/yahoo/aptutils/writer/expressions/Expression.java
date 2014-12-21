package com.yahoo.aptutils.writer.expressions;

import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.writer.JavaFileWriter;

import java.io.IOException;
import java.util.List;

/**
 Expression represent various types of expressions you might find in a typical Java file, and are useful
 * when writing files using {@link com.yahoo.aptutils.writer.JavaFileWriter}. For example--say you want to emit a
 * statement that calls a method on some object, where the argument to that method is a method call on another object.
 * This can be composed entirely using expressions:
 *
 * Expression methodArgument = Expressions.callMethodOn("object2", "anotherMethod");
 * Expression callMethod = Expressions.callMethodOn("object1", "callMethod", arg);
 * writer.writeExpression(callMethod);
 *
 * This would output the following in your generated Java file:
 *
 * object1.anotherMethod(object2.someMethod());
 *
 * Expressions can also be constructed by calling chaining methods on an Expression object. For example:
 *
 * Expression returnExpr = callMethod.returnExpr();
 * writer.writeExpression(returnExpr);
 *
 * Would output:
 *
 * return object1.anotherMethod(object2.someMethod());
 *
 * Other kinds of expressions include variable assignments, casts, constructor invocations,
 * return statements, static method invocations, and more.
 *
 * This class defines various chaining methods you can call to manipulate expressions. For example,
 * if you have an expression representing a method call and want to convert it into a return expression,
 * simply call returnExpr():
 *
 * Expression returnExpr = methodCallExpr.returnExpr();
 */
public abstract class Expression {

    /**
     * @param writer the writer this expression is being written to
     *
     * @return true if anything was written, false otherwise
     *
     * @throws IOException
     */
    public abstract boolean writeExpression(JavaFileWriter writer) throws IOException;

    /**
     * @param methodName name of the method to call
     * @param arguments arguments for the called method
     *
     * @return a new expression representing a method call on this expression
     */
    public Expression callMethod(String methodName, Object... arguments) {
        return Expressions.callMethodOn(this, methodName, arguments);
    }

    /**
     * @param methodName name of the method to call
     * @param arguments arguments for the called method
     *
     * @return a new expression representing a method call on this expression
     */
    public Expression callMethod(String methodName, List<?> arguments) {
        return Expressions.callMethodOn(this, methodName, arguments);
    }

    /**
     * @param fieldName name of the field to reference
     *
     * @return a new expression representing a field reference on this expression
     */
    public Expression reference(String fieldName) {
        return Expressions.reference(this, fieldName);
    }

    /**
     * @return a new expression representing "return <this expression>"
     */
    public Expression returnExpr() {
        return Expressions.returnExpr(this);
    }

    /**
     * @param castTo the type name to cast to
     *
     * @return a new expression representing a cast of this expression
     */
    public Expression cast(TypeName castTo) {
        return Expressions.cast(castTo, this);
    }
    
}
