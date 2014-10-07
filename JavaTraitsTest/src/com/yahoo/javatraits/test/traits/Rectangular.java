package com.yahoo.javatraits.test.traits;

import com.yahoo.javatraits.annotations.Trait;

@Trait
public abstract class Rectangular {

    public abstract int getWidth();
    
    public abstract int getHeight();
    
    public int getArea() {
        return getWidth() * getHeight();
    }
    
    public int getVolumeWithHeight(int height) {
        return getArea() * height;
    }
    
    public int getPerimeter() {
        return 2 * (getWidth() + getHeight());
    }
    
    public boolean isSquare() {
        return getWidth() == getHeight();
    }
    
    public double getDiagonal() {
        return Math.sqrt((getWidth() * getWidth()) + (getHeight() * getHeight()));
    }
}
