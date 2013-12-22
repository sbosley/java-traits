package com.sambosley.javatraits.processor.visitors;

import java.util.Set;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import javax.tools.Diagnostic.Kind;

public class ImportGatheringTypeVisitor implements TypeVisitor<Void, Set<String>> {

	private Element elem;
	private Messager messager;
	
	public ImportGatheringTypeVisitor(Element elem, Messager messager) {
		this.elem = elem;
		this.messager = messager;
	}
	
	@Override
	public Void visit(TypeMirror t) {
		messager.printMessage(Kind.WARNING, "No arg visit() called accumulating imports", elem);
		return null;
	}

	@Override
	public Void visit(TypeMirror t, Set<String> p) {
		t.accept(this, p);
		messager.printMessage(Kind.WARNING, "Generic visit() called accumulating imports", elem);
		return null;
	}

	@Override
	public Void visitArray(ArrayType t, Set<String> p) {
		t.getComponentType().accept(this, p);
		return null;
	}

	@Override
	public Void visitDeclared(DeclaredType t, Set<String> p) {
		p.add(t.toString());
		return null;
	}

	@Override
	public Void visitError(ErrorType t, Set<String> p) {
		messager.printMessage(Kind.ERROR, "Encountered ErrorType accumulating imports", t.asElement());
		return null;
	}

	@Override
	public Void visitExecutable(ExecutableType t, Set<String> p) {
		messager.printMessage(Kind.ERROR, "Encountered ExecutableType accumulating imports", elem);
		return null;
	}

	@Override
	public Void visitNoType(NoType t, Set<String> p) {
		return null; // Nothing to do
	}

	@Override
	public Void visitNull(NullType t, Set<String> p) {
		return null; // Nothing to do
	}

	@Override
	public Void visitPrimitive(PrimitiveType t, Set<String> p) {
		return null; // Nothing to do
	}

	@Override
	public Void visitTypeVariable(TypeVariable t, Set<String> p) {
		messager.printMessage(Kind.WARNING, "Encountered TypeVariable accumulating imports", t.asElement());
		return null;
	}

	@Override
	public Void visitUnknown(TypeMirror t, Set<String> p) {
		messager.printMessage(Kind.WARNING, "Encountered unknown TypeMirror accumulating imports", elem);
		return null;
	}

	@Override
	public Void visitWildcard(WildcardType t, Set<String> p) {
		TypeMirror extendsBound = t.getExtendsBound();
		if (extendsBound != null)
			extendsBound.accept(this, p);
		TypeMirror superBound = t.getSuperBound();
		if (superBound != null)
			superBound.accept(this, p);
		return null;
	}
}
