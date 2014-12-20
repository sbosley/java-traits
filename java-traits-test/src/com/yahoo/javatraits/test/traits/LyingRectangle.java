package com.yahoo.javatraits.test.traits;

import com.yahoo.javatraits.annotations.HasTraits;

@HasTraits(traits=Rectangular.class)
public class LyingRectangle extends LyingRectangleWithTraits {

    @Override
    public int getWidth() {
        return 1;
    }

    @Override
    public int getHeight() {
        return 1;
    }
    
    @Override
    public int getArea() {
        return 0;
    }
    
}
