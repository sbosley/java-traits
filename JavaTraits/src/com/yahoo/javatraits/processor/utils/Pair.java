/**
 * Copyright 2014 Yahoo Inc.
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.javatraits.processor.utils;

public class Pair<L, R> {

    private final L left;
    private final R right;
    
    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }
    
    public L getLeft() {
        return left;
    }
    
    public R getRight() {
        return right;
    }
    
    public static <L, R> Pair<L, R> create(L left, R right) {
        return new Pair<L, R>(left, right);
    }
}
