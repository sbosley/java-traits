package com.sambosley.javatraits.test;

import com.sambosley.javatraits.annotations.HasTraits;

@HasTraits(traits={TestGenerateTrait.class}, desiredSuperclass=String.class)
public class ClassThatHasTraits {

}
