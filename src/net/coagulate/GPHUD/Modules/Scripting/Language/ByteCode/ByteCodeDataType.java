package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSCastException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ByteCodeDataType extends ByteCode {
	
	protected ByteCodeDataType(final ParseNode node) {
		super(node);
	}
	
	// ---------- INSTANCE ----------
	@Nullable
	public ByteCodeDataType add(@Nonnull final ByteCodeDataType var) {
		return new BCString(node(),toString()+var);
	}
	
	@Nullable
	public ByteCodeDataType subtract(@Nonnull final ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can not subtract using type "+var.getClass().getSimpleName(),true);
	}
	
	@Nullable
	public ByteCodeDataType multiply(@Nonnull final ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can not multiply using type "+var.getClass().getSimpleName(),true);
	}
	
	@Nullable
	public ByteCodeDataType divide(@Nonnull final ByteCodeDataType var) {
		throw new GSInvalidExpressionException("Can not divide using type "+var.getClass().getSimpleName(),true);
	}
	
	@Nonnull
	public BCList toBCList() {
		if (getClass().equals(BCList.class)) {
			return (BCList)this;
		}
		return new BCList(node(),this);
	}
	
	public int toInteger() {
		return toBCInteger().getContent();
	}
	
	@Nonnull
	public BCInteger toBCInteger() {
		if (getClass().equals(BCInteger.class)) {
			return (BCInteger)this;
		}
		throw new GSCastException("Can not cast "+getClass().getSimpleName()+" to BCInteger",true);
	}
	
	public float toFloat() {
		return toBCFloat().getContent();
	}
	
	@Nonnull
	public BCFloat toBCFloat() {
		if (getClass().equals(BCFloat.class)) {
			return (BCFloat)this;
		}
		throw new GSCastException("Can not cast "+getClass().getSimpleName()+" to BCFloat",true);
	}
	
	@Nonnull
	public String toString() {
		return toBCString().getContent();
	}
	
	@Nullable
	public abstract ByteCodeDataType clone();
	
	@Nonnull
	public BCString toBCString() {
		if (getClass().equals(BCString.class)) {
			return (BCString)this;
		}
		throw new GSCastException("Can not cast "+getClass().getSimpleName()+" to BCString",true);
	}
	
	public void stack(@Nonnull final GSVM vm) {
		vm.push(this);
	}
	
	/** Compares the contents, true if equals.  Requires type match, so no auto casting here thanks */
	public abstract boolean strictlyEquals(final ByteCodeDataType find);
}
