package com.sambosley.javatraits.utils;

public class GenericName implements TypeName {

    private String genericName;
    private TypeName upperBound;
    
    public GenericName(String genericName, TypeName upperBound) {
        this.genericName = genericName;
        this.upperBound = upperBound;
    }
    
    public String getGenericName() {
        return genericName;
    }
    
    public TypeName getUpperBound() {
        return upperBound;
    }
    
    public void addQualifier(String qualifier) {
        this.genericName = qualifier + "$" + genericName;
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
                + ((upperBound == null) ? 0 : upperBound.hashCode());
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
        if (upperBound == null) {
            if (other.upperBound != null)
                return false;
        } else if (!upperBound.equals(other.upperBound))
            return false;
        return true;
    }
    
}
