package com.yahoo.javatraits.test.traits;

import com.yahoo.javatraits.annotations.DesiredSuperclass;
import com.yahoo.javatraits.annotations.HasTraits;

import java.util.ArrayList;

@HasTraits(traits=BetterList.class,
        desiredSuperclass=@DesiredSuperclass(superclass=ArrayList.class, typeArgNames = "BetterList_T"))
public class BetterArrayList<T> extends BetterArrayListWithTraits<T> {
}
