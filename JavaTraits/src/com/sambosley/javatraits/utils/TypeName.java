package com.sambosley.javatraits.utils;

public interface TypeName {

    public static interface TypeNameVisitor<RET, PARAM> {
        public RET visitClassName(ClassName typeName, PARAM param);
        public RET visitGenericName(GenericName genericName, PARAM param);
    }
    
    public <RETURN, PARAMETER> RETURN accept(TypeNameVisitor<RETURN, PARAMETER> visitor, PARAMETER data);
    
}
