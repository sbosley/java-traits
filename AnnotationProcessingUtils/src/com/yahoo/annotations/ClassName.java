/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.annotations;

import java.util.List;


public class ClassName extends TypeName {

    private String packageName;
    private String simpleName;
    private List<TypeName> typeArgs = null;

    public ClassName(String packageName, String simpleName) {
        this.packageName = packageName;
        this.simpleName = simpleName;
    }

    public ClassName(String fullyQualifiedName) {
        this.packageName = Utils.getPackageFromFullyQualifiedName(fullyQualifiedName);
        this.simpleName = Utils.getSimpleNameFromFullyQualifiedName(fullyQualifiedName);
    }

    @Override
    public ClassName clone() {
        ClassName clone = (ClassName) super.clone();
        clone.packageName = this.packageName;
        clone.simpleName = this.simpleName;
        clone.typeArgs = this.typeArgs;
        return clone;
    }

    public boolean isJavaLangPackage() {
        return "java.lang".equals(packageName);
    }

    public List<TypeName> getTypeArgs() {
        return typeArgs;
    }

    public void setTypeArgs(List<TypeName> typeArgs) {
        this.typeArgs = typeArgs;
    }

    public String toString() {
        if (Utils.isEmpty(packageName))
            return simpleName;
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
        ClassName other = (ClassName) obj;
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
