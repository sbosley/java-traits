package com.sambosley.javatraits.utils;

public class Utils {

	public static String getPackageFromFullyQualifiedName(String name) {
		int split = getFQNSplitIndex(name);
		if (split < 0)
			return "";
		return name.substring(0, split);
	}
	
	public static String getSimpleNameFromFullyQualifiedName(String name) {
		int split = getFQNSplitIndex(name);
		if (split < 0)
			return name;
		return name.substring(split + 1);
	}
	
	public static Pair<String, String> splitFullyQualifiedName(String name) {
		return Pair.create(getPackageFromFullyQualifiedName(name),
				getSimpleNameFromFullyQualifiedName(name));
	}
	
	private static int getFQNSplitIndex(String name) {
		return name.lastIndexOf('.');
	}
	
}
