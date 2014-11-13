/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.annotations.model;

import com.yahoo.annotations.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class GenericName extends TypeName {

    public static final String WILDCARD_CHAR = "?";
    public static final String GENERIC_QUALIFIER_SEPARATOR = "_";
    
    public static final GenericName DEFAULT_WILDCARD = new GenericName(WILDCARD_CHAR, null, null);
    
    private String qualifier;
    private String genericName;
    private List<? extends TypeName> extendsBound;
    private TypeName superBound;

    public GenericName(String genericName, List<TypeName> upperBound, TypeName superBound) {
        this.genericName = genericName;
        this.extendsBound = upperBound;
        this.superBound = superBound;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GenericName clone() {
        GenericName clone = (GenericName) super.clone();
        clone.qualifier = this.qualifier;
        clone.genericName = this.genericName;
        clone.extendsBound = this.extendsBound == null ? null : new ArrayList<TypeName>();
        if (extendsBound != null) {
            for (TypeName t : extendsBound) {
                ((List<TypeName>) clone.extendsBound).add(t);
            }
        }
        clone.superBound = superBound.clone();
        return clone;
    }

    public String getGenericName() {
        StringBuilder result = new StringBuilder();
        if (qualifier != null && !WILDCARD_CHAR.equals(genericName)) {
            result.append(qualifier).append(GENERIC_QUALIFIER_SEPARATOR);
        }
        result.append(genericName);
        return result.toString();
    }

    public void renameTo(String newName) {
        this.genericName = newName;
        this.qualifier = null;
    }

    public boolean isWildcard() {
        return WILDCARD_CHAR.equals(genericName);
    }

    public boolean hasExtendsBound() {
        return extendsBound != null && extendsBound.size() > 0;
    }

    public List<? extends TypeName> getExtendsBound() {
        return extendsBound;
    }

    public void setExtendsBound(List<? extends TypeName> newExtendsBound) {
        this.extendsBound = newExtendsBound;
    }

    public boolean hasSuperBound() {
        return superBound != null;
    }

    public TypeName getSuperBound() {
        return superBound;
    }

    public void setSuperBound(TypeName newSuperBound) {
        this.superBound = newSuperBound;
    }

    public void addQualifier(String qualifier) {
        if (this.qualifier != null) {
            throw new IllegalArgumentException("Generic " + genericName + " already has qualifier " + this.qualifier);
        }
        if (!WILDCARD_CHAR.equals(genericName)) {
            this.qualifier = qualifier;
        }
    }

    @Override
    public <RET, PARAM> RET accept(TypeNameVisitor<RET, PARAM> visitor, PARAM data) {
        return visitor.visitGenericName(this, data);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((getGenericName() == null) ? 0 : getGenericName().hashCode());
        result = prime * result
                + ((extendsBound == null) ? 0 : extendsBound.hashCode());
        result = prime * result
                + ((superBound == null) ? 0 : superBound.hashCode());
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
        GenericName other = (GenericName) obj;
        if (getGenericName() == null) {
            if (other.getGenericName() != null) {
                return false;
            }
        } else if (!getGenericName().equals(other.getGenericName())) {
            return false;
        }
        if (extendsBound == null) {
            if (other.extendsBound != null) {
                return false;
            }
        } else if (!Utils.deepCompareTypeList(extendsBound, other.extendsBound)) {
            return false;
        }
        if (superBound == null) {
            if (other.superBound != null) {
                return false;
            }
        } else if (!superBound.equals(other.superBound)) {
            return false;
        }
        return true;
    }

}
