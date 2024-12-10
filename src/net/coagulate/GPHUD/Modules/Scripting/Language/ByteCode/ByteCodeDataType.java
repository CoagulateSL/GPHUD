package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ByteCodeDataType extends ByteCode {
	
	protected ByteCodeDataType(final ParseNode node) {
		super(node);
	}
	
	public static BCInteger toBoolean(final boolean expression) {
		if (expression) { return truth(); } else { return falseness(); }
	}
	
	protected GSInvalidExpressionException fail(@Nonnull final ByteCodeDataType v) {
		final StackTraceElement[] trace=Thread.currentThread().getStackTrace();
		String methodname="UNKNOWN";
		if (trace.length>1) {
			methodname=trace[trace.length-1].getMethodName();
		}
		return new GSInvalidExpressionException(
				"Operation "+methodname+" can not be applied to "+this.getClass().getSimpleName()+" and "+
				v.getClass().getSimpleName());
	}
	
	protected GSInvalidExpressionException fail() throws GSInvalidExpressionException{
		final StackTraceElement[] trace=Thread.currentThread().getStackTrace();
		String methodname="UNKNOWN";
		if (trace.length>1) {
			methodname=trace[trace.length-1].getMethodName();
		}
		return new GSInvalidExpressionException(
				"Operation "+methodname+" can not be applied to "+this.getClass().getSimpleName());
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	public abstract ByteCodeDataType add(@Nonnull final ByteCodeDataType var);
	
	@Nonnull
	public abstract ByteCodeDataType subtract(@Nonnull final ByteCodeDataType var);
	
	@Nonnull
	public abstract ByteCodeDataType multiply(@Nonnull final ByteCodeDataType var);
	
	@Nonnull
	public abstract ByteCodeDataType divide(@Nonnull final ByteCodeDataType var);
	
	public abstract ByteCodeDataType unaryMinus();
	
	@Nonnull
	public BCInteger logicalOr(@Nonnull final ByteCodeDataType var) {
		if (toBoolean()||var.toBoolean()) {
			return truth();
		}
		return falseness();
	}
	
	public abstract boolean toBoolean();
	
	protected static final BCInteger truth() {
		return new BCInteger(null,1);
	}
	
	protected static final BCInteger falseness() {
		return new BCInteger(null,0);
	}
	
	@Nonnull
	public BCInteger logicalAnd(@Nonnull final ByteCodeDataType var) {
		if (toBoolean()&&var.toBoolean()) {
			return truth();
		}
		return falseness();
	}
	
	public abstract BCInteger not();
	
	@Nonnull
	public BCInteger lessThanOrEqual(@Nonnull final ByteCodeDataType var) {
		if (lessThan(var).toBoolean()||valueEquals(var).toBoolean()) {
			return truth();
		}
		return falseness();
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
	public abstract BCInteger lessThan(@Nonnull final ByteCodeDataType var);
	
	public float toFloat() {
		return toBCFloat().getContent();
	}
	
	@Nonnull
	public abstract BCInteger valueEquals(@Nonnull final ByteCodeDataType var);
	
	@Nonnull
	public String toString() {
		return toBCString().getContent();
	}
	
	@Nullable
	public abstract ByteCodeDataType clone();
	
	@Nonnull
	public BCInteger greaterThanOrEqual(@Nonnull final ByteCodeDataType var) {
		if (greaterThan(var).toBoolean()||valueEquals(var).toBoolean()) {
			return truth();
		}
		return falseness();
	}
	
	@Nonnull
	public abstract BCInteger greaterThan(@Nonnull final ByteCodeDataType var);
	
	@Nonnull
	public abstract BCInteger toBCInteger();
	
	/** Compares the contents, true if equals.  Requires type match, so no auto casting here thanks */
	public abstract boolean strictlyEquals(final ByteCodeDataType find);
	
	@Nonnull
	public abstract BCFloat toBCFloat();
	
	@Nonnull
	public abstract BCString toBCString();
}
