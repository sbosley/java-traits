package com.sambosley.javatraits.processor.data;

import com.sambosley.javatraits.utils.FullyQualifiedName;

public class PreferValue {

    private FullyQualifiedName target;
    private String method;
    
    public PreferValue(FullyQualifiedName target, String method) {
        this.target = target;
        this.method = method;
    }
    
    public FullyQualifiedName getTarget() {
        return target;
    }
    
    public String getMethod() {
        return method;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        result = prime * result + ((target == null) ? 0 : target.hashCode());
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
        PreferValue other = (PreferValue) obj;
        if (method == null) {
            if (other.method != null)
                return false;
        } else if (!method.equals(other.method))
            return false;
        if (target == null) {
            if (other.target != null)
                return false;
        } else if (!target.equals(other.target))
            return false;
        return true;
    }
    
    
}
