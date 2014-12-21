/**
 * Copyright 2014 Yahoo Inc.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.yahoo.aptutils.visitors;

import com.yahoo.aptutils.model.DeclaredTypeName;
import com.yahoo.aptutils.utils.AptUtils;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.type.*;
import javax.tools.Diagnostic.Kind;
import java.util.List;
import java.util.Set;

/**
 * A {@link javax.lang.model.type.TypeVisitor} used to accumulate required imports from
 * {@link javax.lang.model.type.TypeMirror}s. Basically just includes any class referenced by the
 * {@link javax.lang.model.type.TypeMirror} (e.g. itself, upper/lower bounds, etc.)
 */
public class ImportGatheringTypeMirrorVisitor implements TypeVisitor<Void, Set<DeclaredTypeName>> {

    private Element elem;
    private Messager messager;
    private AptUtils aptUtils;

    public ImportGatheringTypeMirrorVisitor(Element elem, Messager messager, AptUtils aptUtils) {
        this.elem = elem;
        this.messager = messager;
        this.aptUtils = aptUtils;
    }

    @Override
    public Void visit(TypeMirror t) {
        messager.printMessage(Kind.WARNING, "No arg visit() called accumulating imports", elem);
        return null;
    }

    @Override
    public Void visit(TypeMirror t, Set<DeclaredTypeName> p) {
        t.accept(this, p);
        messager.printMessage(Kind.WARNING, "Generic visit() called accumulating imports", elem);
        return null;
    }

    @Override
    public Void visitArray(ArrayType t, Set<DeclaredTypeName> p) {
        t.getComponentType().accept(this, p);
        return null;
    }

    @Override
    public Void visitDeclared(DeclaredType t, Set<DeclaredTypeName> p) {
        String toAdd = t.toString();
        if (!AptUtils.OBJECT_CLASS_NAME.equals(toAdd)) {
            String mirrorString = t.toString().replaceAll("<.*>", "");
            p.add(new DeclaredTypeName(mirrorString));
        }
        List<? extends TypeMirror> typeArgs = t.getTypeArguments();
        for (TypeMirror m : typeArgs) {
            m.accept(this, p);
        }
        return null;
    }

    @Override
    public Void visitError(ErrorType t, Set<DeclaredTypeName> p) {
        messager.printMessage(Kind.WARNING, "Encountered ErrorType accumulating imports", t.asElement());
        return null;
    }

    @Override
    public Void visitExecutable(ExecutableType t, Set<DeclaredTypeName> p) {
        TypeMirror returnType = t.getReturnType();
        returnType.accept(this, p);
        List<? extends TypeMirror> parameters = t.getParameterTypes();
        for (TypeMirror var : parameters) {
            var.accept(this, p);
        }
        List<? extends TypeMirror> thrownTypes = t.getThrownTypes();
        for (TypeMirror thrown : thrownTypes) {
            thrown.accept(this, p);
        }
        return null;
    }

    @Override
    public Void visitNoType(NoType t, Set<DeclaredTypeName> p) {
        return null; // Nothing to do
    }

    @Override
    public Void visitNull(NullType t, Set<DeclaredTypeName> p) {
        return null; // Nothing to do
    }

    @Override
    public Void visitPrimitive(PrimitiveType t, Set<DeclaredTypeName> p) {
        return null; // Nothing to do
    }

    @Override
    public Void visitTypeVariable(TypeVariable t, Set<DeclaredTypeName> p) {
        List<? extends TypeMirror> upperBounds = aptUtils.getUpperBoundMirrors(t, t.getUpperBound());
        for (TypeMirror upper : upperBounds) {
            upper.accept(this, p);
        }
        t.getLowerBound().accept(this, p);
        return null;
    }

    @Override
    public Void visitUnknown(TypeMirror t, Set<DeclaredTypeName> p) {
        messager.printMessage(Kind.WARNING, "Encountered unknown TypeMirror accumulating imports", elem);
        return null;
    }

    @Override
    public Void visitWildcard(WildcardType t, Set<DeclaredTypeName> p) {
        List<? extends TypeMirror> upperBounds = aptUtils.getUpperBoundMirrors(t, t.getExtendsBound());
        for (TypeMirror upper : upperBounds) {
            upper.accept(this, p);
        }
        TypeMirror superBound = t.getSuperBound();
        if (superBound != null) {
            superBound.accept(this, p);
        }
        return null;
    }
}
