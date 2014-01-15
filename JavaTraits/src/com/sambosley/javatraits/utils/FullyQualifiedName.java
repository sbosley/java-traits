/**
 * Copyright 2014 Sam Bosley
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.sambosley.javatraits.utils;

public class FullyQualifiedName implements TypeName {

    private String packageName;
    private String simpleName;

    public FullyQualifiedName(String packageName, String simpleName) {
        this.packageName = packageName;
        this.simpleName = simpleName;
    }

    public FullyQualifiedName(String fullyQualifiedName) {
        this.packageName = Utils.getPackageFromFullyQualifiedName(fullyQualifiedName);
        this.simpleName = Utils.getSimpleNameFromFullyQualifiedName(fullyQualifiedName);
    }

    public String toString() {
        return packageName + "." + simpleName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getSimpleName() {
        return simpleName;
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
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FullyQualifiedName other = (FullyQualifiedName) obj;
        if (packageName == null) {
            if (other.packageName != null)
                return false;
        } else if (!packageName.equals(other.packageName))
            return false;
        if (simpleName == null) {
            if (other.simpleName != null)
                return false;
        } else if (!simpleName.equals(other.simpleName))
            return false;
        return true;
    }

    @Override
    public <RETURN, PARAMETER> RETURN accept(
            TypeNameVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
        return visitor.visitClassName(this, data);
    }

}
