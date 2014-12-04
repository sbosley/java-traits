package com.yahoo.javatraits.test.traits;

import com.yahoo.javatraits.annotations.Trait;

import java.util.List;

@Trait
public abstract class BetterList<T extends CharSequence> implements List<T> {

    public void printAll() {
        for (int i = 0; i < size(); i++) {
            T item = get(i);
            System.out.println("Item " + item);
        }
    }
}
