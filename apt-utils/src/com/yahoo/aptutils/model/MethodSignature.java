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

import com.yahoo.aptutils.utils.AptUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a method element. The four relevant pieces of a method signature for comparing to other methods
 * are considered to be: method name, return type, argument types, and throws types
 */
public class MethodSignature {

    private String methodName;
    private TypeName returnType;
    private List<TypeName> argTypes = new ArrayList<TypeName>();
    private List<TypeName> throwsTypes = new ArrayList<TypeName>();

    public MethodSignature(String methodName) {
        this.methodName = methodName;
    }

    /**
     * @return the name of this method
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Set the method name
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * @return this method's return type
     */
    public TypeName getReturnType() {
        return returnType;
    }

    /**
     * Sets this method's return type
     */
    public void setReturnType(TypeName returnType) {
        this.returnType = returnType;
    }

    /**
     * @return the type arguments local to this method
     */
    public List<TypeName> getArgTypes() {
        return argTypes;
    }

    /**
     * Add type arguments local to this method
     */
    public void addArgTypes(TypeName... types) {
        addArgTypes(AptUtils.asList(types));
    }

    /**
     * Add type arguments local to this method
     */
    public void addArgTypes(List<? extends TypeName> types) {
        if (types != null) {
            argTypes.addAll(types);
        }
    }

    /**
     * @return the types this method may throw
     */
    public List<TypeName> getThrowsTypes() {
        return throwsTypes;
    }

    /**
     * Add thrown types to this method
     */
    public void addThrowsTypes(TypeName... types) {
        addThrowsTypes(AptUtils.asList(types));
    }

    /**
     * Add thrown types to this method
     */
    public void addThrowsTypes(List<? extends TypeName> types) {
        if (types != null) {
            throwsTypes.addAll(throwsTypes);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((argTypes == null) ? 0 : argTypes.hashCode());
        result = prime * result
                + ((methodName == null) ? 0 : methodName.hashCode());
        result = prime * result
                + ((returnType == null) ? 0 : returnType.hashCode());
        result = prime * result
                + ((throwsTypes == null) ? 0 : throwsTypes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MethodSignature other = (MethodSignature) obj;
        if (!AptUtils.deepCompareTypeList(argTypes, other.argTypes)) {
            return false;
        }
        if (!AptUtils.isEqual(methodName, other.methodName)) {
            return false;
        }
        if (!AptUtils.deepCompareTypes(returnType, other.returnType)) {
            return false;
        }
        if (!AptUtils.deepCompareTypeList(throwsTypes, other.throwsTypes)) {
            return false;
        }
        return true;
    }


}
