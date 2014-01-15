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

    @Override
    public <RETURN, PARAMETER> RETURN accept(
            TypeNameVisitor<RETURN, PARAMETER> visitor, PARAMETER data) {
        return visitor.visitGenericName(this, data);
    }
    
}
