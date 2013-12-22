package com.sambosley.javatraits.utils;

public class Pair<A, B> {

	private final A left;
	private final B right;
	
	public Pair(A left, B right) {
		this.left = left;
		this.right = right;
	}
	
	public A getLeft() {
		return left;
	}
	
	public B getRight() {
		return right;
	}
	
	public static <L, R> Pair<L, R> create(L left, R right) {
		return new Pair<L, R>(left, right);
	}
	
}
