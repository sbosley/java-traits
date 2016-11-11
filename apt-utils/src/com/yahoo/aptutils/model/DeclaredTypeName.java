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

import java.util.List;

/**
 * Represents a concrete type name (e.g. "java.lang.String")
 *
 * May contain information about type arguments that can be used when generating
 * code in {@link com.yahoo.aptutils.writer.JavaFileWriter}, but is not considered
 * when comparing two DeclaredTypeNames to each other. For example, a DeclaredTypeName
 * representing "List<String>" will equal a DeclaredTypeName representing "List<Integer>".
 * For a deeper equality comparison, use {@link com.yahoo.aptutils.utils.AptUtils#deepCompareTypes(TypeName, TypeName)}
 */
public class DeclaredTypeName extends TypeName {

    private String packageName;
    private String simpleName;
    private List<? extends TypeName> typeArgs = null;

    public DeclaredTypeName(String packageName, String simpleName) {
        this.packageName = packageName;
        this.simpleName = simpleName;
    }

    public DeclaredTypeName(String fullyQualifiedName) {
        this(AptUtils.getPackageFromFullyQualifiedName(fullyQualifiedName),
                AptUtils.getSimpleNameFromFullyQualifiedName(fullyQualifiedName));
    }

    @Override
    public DeclaredTypeName clone() {
        DeclaredTypeName clone = (DeclaredTypeName) super.clone();
        clone.packageName = this.packageName;
        clone.simpleName = this.simpleName;
        clone.typeArgs = this.typeArgs;
        return clone;
    }

    /**
     * @return true if this type is in the java.lang package
     */
    public boolean isJavaLangPackage() {
        return CoreTypes.JAVA_LANG.equals(packageName);
    }

    /**
     * @return the list of type arguments for this type
     */
    public List<? extends TypeName> getTypeArgs() {
        return typeArgs;
    }

    /**
     * @param typeArgs type arguments to set for this type
     */
    public void setTypeArgs(List<? extends TypeName> typeArgs) {
        this.typeArgs = typeArgs;
    }

    /**
     * @return if the type has a package, returns "packageName.simpleName". Otherwise, returns simpleName
     */
    @Override
    public String toString() {
        if (AptUtils.isEmpty(packageName)) {
            return simpleName;
        }
        return packageName + "." + simpleName;
    }

    /**
     * @return the name of the package this type is in
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * @return the simple name of this type (not including package name)
     */
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
