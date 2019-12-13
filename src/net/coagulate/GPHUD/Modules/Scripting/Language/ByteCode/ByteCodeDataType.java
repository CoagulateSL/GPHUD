package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSCastException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ByteCodeDataType extends ByteCode {

	public ByteCodeDataType(ParseNode n) {
		super(n);
	}

	@Nullable
	public ByteCodeDataType add(@Nonnull ByteCodeDataType var) { return new BCString(node(),toString()+var.toString()); }
	@Nullable
	public ByteCodeDataType subtract(@Nonnull ByteCodeDataType var) { throw new GSInvalidExpressionException("Can not subtract using type "+var.getClass().getSimpleName()); }
	@Nullable
	public ByteCodeDataType multiply(@Nonnull ByteCodeDataType var) { throw new GSInvalidExpressionException("Can not multiply using type "+var.getClass().getSimpleName()); }
	@Nullable
	public ByteCodeDataType divide(@Nonnull ByteCodeDataType var) { throw new GSInvalidExpressionException("Can not divide using type "+var.getClass().getSimpleName()); }

	@Nullable
	public BCString toBCString() {
		if (this.getClass().equals(BCString.class)) { return (BCString)this; }
		throw new GSCastException("Can not cast "+this.getClass().getSimpleName()+" to BCString");
	}
	@Nullable
	public BCInteger toBCInteger() {
		if (this.getClass().equals(BCInteger.class)) { return (BCInteger)this; }
		throw new GSCastException("Can not cast "+this.getClass().getSimpleName()+" to BCInteger");
	}
	@Nullable
	public BCList toBCList() {
		if (this.getClass().equals(BCList.class)) { return (BCList)this; } return new BCList(node(),this); }

	@Nonnull
	public String toString() { return toBCString().getContent(); }
	public int toInteger() { return toBCInteger().getContent(); }

	@Nullable
	public abstract ByteCodeDataType clone();

	public void stack(@Nonnull GSVM vm) {
		vm.push(this);
	}
}
