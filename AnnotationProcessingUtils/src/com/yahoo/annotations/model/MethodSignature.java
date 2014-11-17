/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.annotations.model;

import com.yahoo.annotations.utils.AptUtils;

import java.util.ArrayList;
import java.util.List;

public class MethodSignature {

    private String methodName;
    private TypeName returnType;
    private List<TypeName> argTypes = new ArrayList<TypeName>();
    private List<TypeName> throwsTypes = new ArrayList<TypeName>();

    public MethodSignature(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public TypeName getReturnType() {
        return returnType;
    }

    public void setReturnType(TypeName returnType) {
        this.returnType = returnType;
    }

    public List<TypeName> getArgTypes() {
        return argTypes;
    }

    public void addArgTypes(TypeName... types) {
        addArgTypes(AptUtils.asList(types));
    }

    public void addArgTypes(List<? extends TypeName> types) {
        if (types != null) {
            argTypes.addAll(types);
        }
    }

    public List<TypeName> getThrowsTypes() {
        return throwsTypes;
    }

    public void addThrowsTypes(TypeName... types) {
        addThrowsTypes(AptUtils.asList(types));
    }

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
