/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.annotations.model;

import java.util.ArrayList;
import java.util.List;

import com.yahoo.annotations.utils.Utils;

public class GenericName extends TypeName {

    public static final String WILDCARD_CHAR = "?";
    public static final String GENERIC_QUALIFIER_SEPARATOR = "_";
    
    public static final GenericName DEFAULT_WILDCARD = new GenericName(WILDCARD_CHAR, null, null);
    
    private String qualifier;
    private String genericName;
    private List<TypeName> extendsBound;
    private TypeName superBound;

    public GenericName(String genericName, List<TypeName> upperBound, TypeName superBound) {
        this.genericName = genericName;
        this.extendsBound = upperBound;
        this.superBound = superBound;
    }

    @Override
    public GenericName clone() {
        GenericName clone = (GenericName) super.clone();
        clone.qualifier = this.qualifier;
        clone.genericName = this.genericName;
        clone.extendsBound = this.extendsBound == null ? null : new ArrayList<TypeName>();
        if (extendsBound != null) {
            for (TypeName t : extendsBound) {
                clone.extendsBound.add(t);
            }
        }
        clone.superBound = (TypeName) superBound.clone();
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

    public boolean isWildcard() {
        return WILDCARD_CHAR.equals(genericName);
    }

    public boolean hasExtendsBound() {
        return extendsBound != null && extendsBound.size() > 0;
    }

    public List<TypeName> getExtendsBound() {
        return extendsBound;
    }

    public boolean hasSuperBound() {
        return superBound != null;
    }

    public TypeName getSuperBound() {
        return superBound;
    }

    public void addQualifier(String qualifier) {
        if (this.qualifier != null) {
            throw new IllegalArgumentException("Generic " + genericName + " already has qualifier " + this.qualifier);
        }
        this.qualifier = qualifier;
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
                + ((genericName == null) ? 0 : genericName.hashCode());
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
        if (genericName == null) {
            if (other.genericName != null) {
                return false;
            }
        } else if (!genericName.equals(other.genericName)) {
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
