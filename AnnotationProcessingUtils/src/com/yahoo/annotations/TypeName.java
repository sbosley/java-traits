/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.annotations;

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

    public abstract String getTypeString(boolean simple);

    protected void appendArrayString(StringBuilder builder) {
        for (int i = 0; i < arrayDepth; i++) {
            if (i == arrayDepth - 1 && isVarArgs)
                builder.append("...");
            else
                builder.append("[]");
        }
    }

    public static interface TypeNameVisitor<RET, PARAM> {
        public RET visitClassName(ClassName typeName, PARAM param);
        public RET visitGenericName(GenericName genericName, PARAM param);
    }

    public abstract <RETURN, PARAMETER> RETURN accept(TypeNameVisitor<RETURN, PARAMETER> visitor, PARAMETER data);

}
