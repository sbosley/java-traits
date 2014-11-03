package com.yahoo.javatraits.test.traits;

import com.yahoo.javatraits.annotations.Trait;

import java.util.List;

/**
 * Created by Sam on 11/3/14.
 */
@Trait
public abstract class BetterList implements List<String> {

    public void forEach(Runnable run) {
        for (int i = 0; i < size(); i++) {
            String item = get(i);
            run.run();
        }
    }
}
