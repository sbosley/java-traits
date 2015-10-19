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
package com.yahoo.javatraits.test.classes;

import com.yahoo.javatraits.annotations.HasTraits;
import com.yahoo.javatraits.test.traits.Rectangular;

@HasTraits(traits=Rectangular.class)
public class LyingRectangle extends LyingRectangleWithTraits {

    @Override
    public int getWidth() {
        return 1;
    }

    @Override
    public int getHeight() {
        return 1;
    }
    
    @Override
    public int getArea() {
        return 0;
    }

    @HasTraits(traits=Rectangular.class)
    public class InnerRectangle extends InnerRectangleWithTraits{

        @Override
        public int getWidth() {
            return LyingRectangle.this.getWidth()/2;
        }

        @Override
        public int getHeight() {
            return LyingRectangle.this.getHeight()/2;
        }
    }
}
