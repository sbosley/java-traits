/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.annotations;

public class GenericName extends TypeName {

    private String qualifier;
    private String genericName;
    private TypeName extendsBound;
    private TypeName superBound;

    public GenericName(String genericName, TypeName upperBound, TypeName superBound) {
        this.genericName = genericName;
        this.extendsBound = upperBound;
        this.superBound = superBound;
    }

    @Override
    public GenericName clone() {
        GenericName clone = (GenericName) super.clone();
        clone.qualifier = this.qualifier;
        clone.genericName = this.genericName;
        clone.extendsBound = (TypeName) extendsBound.clone();
        clone.superBound = (TypeName) superBound.clone();
        return clone;
    }

    public String getGenericName() {
        StringBuilder result = new StringBuilder();
        if (qualifier != null && !"?".equals(genericName))
            result.append(qualifier).append("$");
        result.append(genericName);
        return result.toString();
    }

    public boolean isWildcard() {
        return "?".equals(genericName);
    }

    public boolean hasExtendsBound() {
        return extendsBound != null;
    }

    public TypeName getExtendsBound() {
        return extendsBound;
    }

    public boolean hasSuperBound() {
        return superBound != null;
    }

    public TypeName getSuperBound() {
        return superBound;
    }

    public void addQualifier(String qualifier) {
        if (this.qualifier != null)
            throw new IllegalArgumentException("Generic " + genericName + " already has qualifier " + this.qualifier);
        this.qualifier = qualifier;
    }

    @Override
    public <RETURN, PARAMETER> RETURN accept(
            TypeNameVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
        return visitor.visitGenericName(this, data);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((genericName == null) ? 0 : genericName.hashCode());
        result = prime * result
                + ((extendsBound == null) ? 0 : extendsBound.hashCode());
        result = prime * result
                + ((superBound == null) ? 0 : superBound.hashCode());
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
        GenericName other = (GenericName) obj;
        if (genericName == null) {
            if (other.genericName != null)
                return false;
        } else if (!genericName.equals(other.genericName))
            return false;
        if (extendsBound == null) {
            if (other.extendsBound != null)
                return false;
        } else if (!extendsBound.equals(other.extendsBound))
            return false;
        if (superBound == null) {
            if (other.superBound != null)
                return false;
        } else if (!superBound.equals(other.superBound))
            return false;
        return true;
    }

}
