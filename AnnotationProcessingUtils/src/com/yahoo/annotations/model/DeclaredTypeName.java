/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.annotations.model;

import com.yahoo.annotations.utils.AptUtils;

import java.util.List;

public class DeclaredTypeName extends TypeName {

    private String packageName;
    private String simpleName;
    private List<? extends TypeName> typeArgs = null;

    public DeclaredTypeName(String packageName, String simpleName) {
        this.packageName = packageName;
        this.simpleName = simpleName;
    }

    public DeclaredTypeName(String fullyQualifiedName) {
        this.packageName = AptUtils.getPackageFromFullyQualifiedName(fullyQualifiedName);
        this.simpleName = AptUtils.getSimpleNameFromFullyQualifiedName(fullyQualifiedName);
    }

    @Override
    public DeclaredTypeName clone() {
        DeclaredTypeName clone = (DeclaredTypeName) super.clone();
        clone.packageName = this.packageName;
        clone.simpleName = this.simpleName;
        clone.typeArgs = this.typeArgs;
        return clone;
    }

    public boolean isJavaLangPackage() {
        return CoreTypes.JAVA_LANG.equals(packageName);
    }

    public List<? extends TypeName> getTypeArgs() {
        return typeArgs;
    }

    public void setTypeArgs(List<? extends TypeName> typeArgs) {
        this.typeArgs = typeArgs;
    }

    public String toString() {
        if (AptUtils.isEmpty(packageName)) {
            return simpleName;
        }
        return packageName + "." + simpleName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public <RET, PARAM> RET accept(TypeNameVisitor<RET, PARAM> visitor, PARAM data) {
        return visitor.visitClassName(this, data);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((packageName == null) ? 0 : packageName.hashCode());
        result = prime * result
                + ((simpleName == null) ? 0 : simpleName.hashCode());
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
        DeclaredTypeName other = (DeclaredTypeName) obj;
        if (packageName == null) {
            if (other.packageName != null) {
                return false;
            }
        } else if (!packageName.equals(other.packageName)) {
            return false;
        }
        if (simpleName == null) {
            if (other.simpleName != null) {
                return false;
            }
        } else if (!simpleName.equals(other.simpleName)) {
            return false;
        }
        return true;
    }
}
