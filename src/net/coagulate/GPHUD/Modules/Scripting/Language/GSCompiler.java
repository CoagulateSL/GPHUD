package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputInvalidChoiceException;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCode;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.*;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * The abstract superclass of all compilers
 * Language implementation versions are:
 * 1 - Original stack VM based implementation using absolute addressing
 * 2 - Stack VM based implementation using relative addressing only ; to permit relocation (for sub functions)
 * 3 - Java compilation based implementation, for speed
 */

public abstract class GSCompiler {
	private final ParseNode startnode;
	private final String    scriptname;
	private final int       sourceVersion;
	protected     State     compiledState=null;
	
	protected GSCompiler(final Node passednode,final String scriptname,final int sourceVersion) {
		if (!(passednode instanceof ParseNode)) {
			throw new SystemImplementationException(
					"Compiler error - passed node is of type "+passednode.getClass().getCanonicalName()+
					" which is not a ParseNode implementation");
		}
		startnode=(ParseNode)passednode;
		this.scriptname=scriptname;
		this.sourceVersion=sourceVersion;
	}
	
	public ParseNode startnode() {
		return startnode;
	}
	
	public String scriptname() {
		return scriptname;
	}
	
	public static GSCompiler create(final String compiler,
	                                final GSStart gsscript,
	                                final String name,
	                                final int sourceVersion) {
		if ("V2-GSStackVM/Relative".equalsIgnoreCase(compiler)) {
			return new GSStackVMCompiler(gsscript,name,sourceVersion);
		}
		if ("V3-GSJavaVM".equalsIgnoreCase(compiler)) {
			return new GSJavaCompiler(gsscript,name,sourceVersion);
		}
		throw new UserInputInvalidChoiceException("Compiler '"+compiler+"'  is not handled");
	}
	
	public int sourceVersion() {
		return sourceVersion;
	}
	
	public State getCompiledState() {
		return compiledState;
	}
	
	public List<ByteCode> compile(final State st) {
		compiledState=st;
		return _compile(st,startnode());
	}
	
	protected abstract List<ByteCode> _compile(final State st,ParseNode node);
	
	public abstract Byte[] toByteCode(final State st);
	
	public abstract int version();
	
	protected int expectedChildren(final ParseNode node) {
		if (node instanceof GSStart) {
			return -1;
		}
		if (node instanceof GSInitialiser) {
			return 3;
		}
		if (node instanceof GSExpression) {
			return 1;
		}
		if (node instanceof GSParameter) {
			return 1;
		}
		if (node instanceof GSTerm) {
			return 1;
		}
		if (node instanceof GSFunctionCall) {
			return -1;
		}
		if (node instanceof GSStringConstant) {
			return 0;
		}
		if (node instanceof GSFloatConstant) {
			return 0;
		}
		if (node instanceof GSIntegerConstant) {
			return 0;
		}
		if (node instanceof GSIdentifier) {
			return 0;
		}
		if (node instanceof GSParameters) {
			return -1;
		}
		if (node instanceof GSConditional) {
			return -1;
		}
		if (node instanceof GSAssignment) {
			return 2;
		}
		if (node instanceof GSStatement) {
			return -1;
		}
		if (node instanceof GSList) {
			return -1;
		}
		if (node instanceof GSListIndex) {
			return 2;
		}
		if (node instanceof GSWhileLoop) {
			return 2;
		}
		if (node instanceof GSLogicalAnd) {
			return -1;
		}
		if (node instanceof GSLogicalOr) {
			return -1;
		}
		if (node instanceof GSInEquality) {
			return -1;
		}
		if (node instanceof GSEquality) {
			return -1;
		}
		if (node instanceof GSGreaterThan) {
			return -1;
		}
		if (node instanceof GSLessThan) {
			return -1;
		}
		if (node instanceof GSGreaterOrEqualThan) {
			return -1;
		}
		if (node instanceof GSLessOrEqualThan) {
			return -1;
		}
		if (node instanceof GSAdd) {
			return -1;
		}
		if (node instanceof GSSubtract) {
			return -1;
		}
		if (node instanceof GSMultiply) {
			return -1;
		}
		if (node instanceof GSDivide) {
			return -1;
		}
		if (node instanceof GSLogicalNot) {
			return 1;
		}
		if (node instanceof GSUnaryMinus) {
			return 1;
		}
		if (node instanceof GSReturn) {
			return -1;
		}
		if (node instanceof GSDiscardExpression) {
			return -1;
		}
		throw new SystemImplementationException(
				"Expected Children not defined for node "+node.getClass().getName()+" at line "+
				node.jjtGetFirstToken().beginLine+", column "+node.jjtGetFirstToken().beginColumn);
	}
	
	
	protected void checkType(@Nonnull final ParseNode node,
	                         final int pos,
	                         @Nonnull final Class<? extends ParseNode> clazz) {
		if (node.jjtGetNumChildren()<pos) {
			throw new GSInternalError(
					"Checking type failed = has "+node.jjtGetNumChildren()+" and we asked for pos_0: "+pos+" in clazz "+
					clazz.getName());
		}
		final Node child=node.jjtGetChild(pos);
		if (!child.getClass().equals(clazz)) {
			throw new GSInternalError(
					"Child_0 "+pos+" of "+node.getClass().getName()+" is of type "+child.getClass().getName()+
					" not the expected "+clazz.getName());
		}
	}
	
}
