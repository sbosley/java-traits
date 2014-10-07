package com.yahoo.javatraits.test.traits;

import com.yahoo.javatraits.annotations.HasTraits;

@HasTraits(traits=Rectangular.class)
public class FootballField extends FootballFieldWithTraits {

    public static final int WIDTH = 160;
    public static final int HEIGHT = 320;
    
    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

}
