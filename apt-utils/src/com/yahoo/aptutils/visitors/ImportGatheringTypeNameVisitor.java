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
package com.yahoo.aptutils.visitors;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.model.GenericName;
import com.yahoo.aptutils.model.TypeName;
import com.yahoo.aptutils.model.TypeName.TypeNameVisitor;

import java.util.List;
import java.util.Set;

/**
 * A {@link com.yahoo.aptutils.model.TypeName.TypeNameVisitor} used to accumulate required imports from
 * {@link com.yahoo.aptutils.model.TypeName}s. Basically just includes any class referenced by the
 * {@link com.yahoo.aptutils.model.TypeName} (e.g. itself, upper/lower bounds, etc.)
 */
public class ImportGatheringTypeNameVisitor implements TypeNameVisitor<Void, Set<DeclaredTypeName>> {

    @Override
    public Void visitClassName(DeclaredTypeName typeName, Set<DeclaredTypeName> imports) {
        imports.add(typeName);
        List<? extends TypeName> typeArgs = typeName.getTypeArgs();
        if (typeArgs != null) {
            for (TypeName arg : typeArgs) {
                arg.accept(this, imports);
            }
        }
        return null;
    }

    @Override
    public Void visitGenericName(GenericName genericName, Set<DeclaredTypeName> imports) {
        List<? extends TypeName> extendsBounds = genericName.getExtendsBound();
        if (extendsBounds != null) {
            for (TypeName extendsBound : extendsBounds) {
                extendsBound.accept(this, imports);
            }
        }
        TypeName superBound = genericName.getSuperBound();
        if (superBound != null) {
            superBound.accept(this, imports);
        }
        return null;
    }
}
