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
package com.yahoo.javatraits.processor;

import com.yahoo.javatraits.annotations.Trait;
import com.yahoo.javatraits.processor.data.TraitElement;
import com.yahoo.javatraits.processor.writers.TraitDelegateWriter;
import com.yahoo.javatraits.processor.writers.TraitInterfaceWriter;

import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;

public class TraitProcessor extends JavaTraitsProcessor<TraitElement> {

    @Override
    protected Class<? extends Annotation> getAnnotationClass() {
        return Trait.class;
    }

    @Override
    protected TraitElement itemFromTypeElement(TypeElement typeElem) {
        return new TraitElement(typeElem, utils);
    }

    @Override
    protected void processItem(TraitElement item) {
        new TraitInterfaceWriter(item, utils).writeClass(filer);
        new TraitDelegateWriter(item, utils).writeClass(filer);
    }
}
