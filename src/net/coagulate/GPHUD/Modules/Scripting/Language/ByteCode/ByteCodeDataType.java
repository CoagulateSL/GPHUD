package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSCastException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;

public abstract class ByteCodeDataType extends ByteCode {

	public ByteCodeDataType add(ByteCodeDataType var) { return new BCString(toString()+var.toString()); }
	public ByteCodeDataType subtract(ByteCodeDataType var) { throw new GSInvalidExpressionException("Can not subtract using type "+var.getClass().getSimpleName()); }
	public ByteCodeDataType multiply(ByteCodeDataType var) { throw new GSInvalidExpressionException("Can not multiply using type "+var.getClass().getSimpleName()); }
	public ByteCodeDataType divide(ByteCodeDataType var) { throw new GSInvalidExpressionException("Can not divide using type "+var.getClass().getSimpleName()); }

	public BCString toBCString() {
		if (this.getClass().equals(BCString.class)) { return (BCString)this; }
		throw new GSCastException("Can not cast "+this.getClass().getSimpleName()+" to BCString");
	}
	public BCInteger toBCInteger() {
		if (this.getClass().equals(BCInteger.class)) { return (BCInteger)this; }
		throw new GSCastException("Can not cast "+this.getClass().getSimpleName()+" to BCInteger");
	}
	public BCList toBCList() {
		if (this.getClass().equals(BCList.class)) { return (BCList)this; } return new BCList(this); }

	public String toString() { return toBCString().getContent(); }
	public int toInteger() { return toBCInteger().getContent(); }

	public abstract ByteCodeDataType clone();
}
