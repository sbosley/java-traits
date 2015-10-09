/*
 * Copyright 2014 Yahoo Inc.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yahoo.javatraits.test;

import com.yahoo.javatraits.test.traits.*;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BasicTraitsTest {

    private SomeClass<Number, Integer, String, Long> instance = new SomeClass<Number, Integer, String, Long>();

    @Test
    public void testBasicTraitFunctions() {
        FootballField field = new FootballField();
        
        int expectedPerimeter = 2 * (FootballField.WIDTH + FootballField.HEIGHT);
        assertEquals(expectedPerimeter, field.getPerimeter());
        
        int expectedArea = FootballField.WIDTH * FootballField.HEIGHT;
        assertEquals(expectedArea, field.getArea());
    }
    
    @Test
    public void testOverrideConcreteTraitMethods() {
        // Lying rectangle reports width and height = 1, but area = 0.
        // If overriding concrete trait methods works, getVolume() will
        // always return 0.
        LyingRectangle lyingRect = new LyingRectangle();
        
        assertEquals(0, lyingRect.getArea());
        assertEquals(0, lyingRect.getVolumeWithHeight(1));
        assertEquals(0, lyingRect.getVolumeWithHeight(2));
        assertEquals(0, lyingRect.getVolumeWithHeight(3));
    }
    
    @Test
    public void testSuperclassSpecification() {
        assertTrue(instance instanceof HashMap<?, ?>);
        instance.put("Hello", 1L);
        assertEquals(1L, instance.get("Hello").longValue());
    }
    
    @Test
    public void testTraitClassesImplementAllInterfaces() {
        assertTrue(instance instanceof IMathTrait);
        assertTrue(instance instanceof IAnotherTrait);
    }
    
    @Test
    public void testDiamondResolutionWithoutPrefer() {
        int hexInt = 0xabc;
        String hexString = instance.intToStringV1(hexInt);
        assertEquals("abc", hexString);
    }
    
    @Test
    public void testDiamondResolutionWithPrefer() {
        int hexInt = 0xabc;
        String binaryString = instance.intToStringV2(hexInt);
        assertEquals("101010111100", binaryString);
    }

    @Test
    public void testTraitExtendingInterface() {
        IBetterList<String> list = new BetterArrayList<String>();
        assertTrue(list instanceof List);
        assertTrue(list instanceof IBetterList);
    }

    @Test
    public void testTraitVariables() {
       instance.setTestVariable(5);
        assertEquals(5, instance.getTestVariable());
    }

}
