/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.aptutils.model;

/**
 * Abstract parent class for {@link DeclaredTypeName} and {@link GenericName}
 *
 * Contains information about if the TypeName represents an array or is a varargs type in a method. For example,
 * Integer[][] would be represented by a {@link DeclaredTypeName} "java.lang.Integer"
 * with an array depth of 2.
 */
public abstract class TypeName implements Cloneable {

    private int arrayDepth = 0;
    private boolean isVarArgs = false;

    public void setArrayDepth(int arrayDepth) {
        this.arrayDepth = arrayDepth;
    }

    public int getArrayDepth() {
        return this.arrayDepth;
    }

    public void setIsVarArgs(boolean isVarArgs) {
        this.isVarArgs = isVarArgs;
    }

    public boolean isVarArgs() {
        return isVarArgs;
    }

    @Override
    public TypeName clone() {
        TypeName clone;
        try {
            clone = (TypeName) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        clone.arrayDepth = this.arrayDepth;
        clone.isVarArgs = this.isVarArgs;
        return clone;
    }

    /**
     * @return a string representing the array depth of this type. If the type is not a varargs, the string
     * will be arrayDepth * "[]", otherwise it will be (arrayDepth - 1) * "[]" + "..."
     */
    public String getArrayStringSuffix() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < arrayDepth; i++) {
            if (i == arrayDepth - 1 && isVarArgs) {
                builder.append("...");
            } else {
                builder.append("[]");
            }
        }
        return builder.toString();
    }

    public static interface TypeNameVisitor<RET, PARAM> {
        public RET visitClassName(DeclaredTypeName typeName, PARAM param);
        public RET visitGenericName(GenericName genericName, PARAM param);
    }

    public abstract <RET, PARAM> RET accept(TypeNameVisitor<RET, PARAM> visitor, PARAM data);

}
