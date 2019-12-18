package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSCastException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ByteCodeDataType extends ByteCode {

	public ByteCodeDataType(final ParseNode n) {
		super(n);
	}

	@Nullable
	public ByteCodeDataType add(@Nonnull final ByteCodeDataType var) { return new BCString(node(),toString()+var); }

	@Nullable
	public ByteCodeDataType subtract(@Nonnull final ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can not subtract using type "+var.getClass().getSimpleName());
	}

	@Nullable
	public ByteCodeDataType multiply(@Nonnull final ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can not multiply using type "+var.getClass().getSimpleName());
	}

	@Nullable
	public ByteCodeDataType divide(@Nonnull final ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can not divide using type "+var.getClass().getSimpleName());
	}

	@Nonnull
	public BCString toBCString() {
		if (getClass().equals(BCString.class)) { return (BCString) this; }
		throw new GSCastException("Can not cast "+getClass().getSimpleName()+" to BCString");
	}

	@Nonnull
	public BCInteger toBCInteger() {
		if (getClass().equals(BCInteger.class)) { return (BCInteger) this; }
		throw new GSCastException("Can not cast "+getClass().getSimpleName()+" to BCInteger");
	}

	@Nonnull
	public BCList toBCList() {
		if (getClass().equals(BCList.class)) { return (BCList) this; }
		return new BCList(node(),this);
	}

	@Nonnull
	public String toString() { return toBCString().getContent(); }

	public int toInteger() { return toBCInteger().getContent(); }

	@Nullable
	public abstract ByteCodeDataType clone();

	public void stack(@Nonnull final GSVM vm) {
		vm.push(this);
	}
}
