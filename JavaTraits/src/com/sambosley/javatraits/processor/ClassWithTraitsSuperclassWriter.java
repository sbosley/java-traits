package com.sambosley.javatraits.processor;

import java.util.Map;

import com.sambosley.javatraits.utils.FullyQualifiedName;

public class ClassWithTraitsSuperclassWriter {

	private ClassWithTraits cls;
	private Map<FullyQualifiedName, TraitElement> traitElementMap;
	
	public ClassWithTraitsSuperclassWriter(ClassWithTraits cls, Map<FullyQualifiedName, TraitElement> traitElementMap) {
		this.cls = cls;
		this.traitElementMap = traitElementMap;
	}
	
}
