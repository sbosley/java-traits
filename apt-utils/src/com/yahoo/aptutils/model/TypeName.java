/*
 * Copyright 2014 Yahoo Inc.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
