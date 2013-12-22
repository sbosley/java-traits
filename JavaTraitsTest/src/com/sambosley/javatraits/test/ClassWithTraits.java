package com.sambosley.javatraits.test;

import com.sambosley.javatraits.annotations.HasTraits;

@HasTraits(traits={MyTrait.class})
public class ClassWithTraits extends ClassWithTraitsGen {

	@Override
	public int someWeirdOp(int arg1, int arg2) {
		return 0;
	}

}
