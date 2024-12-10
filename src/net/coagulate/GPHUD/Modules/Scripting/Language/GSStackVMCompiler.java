package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.*;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.*;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class GSStackVMCompiler extends GSCompiler {
	
	/**
	 * Used to note which compilation engine we compiled with, where it matters
	 * 0 - The original (pre relative jumps) compiler, note some operator ordering changed prior to this (version 2 BC ops)
	 * 1 - Relocatable code compiler - this compiler only generates relative jumps, previously we only generated absolute jumps
	 * - Code compiled with version 2 compiler can run at any memory location so supports being included and memory mapped
	 * - Code compiled prior to version 1 uses absolute jumps and will corrupt the script state if relocated from base address 0x0
	 */
	public static final int COMPILER_VERSION=1;
	private             int jumpnumber      =1;
	private             int lastdebuglineno =-1;
	private             int lastdebugcolno  =-1;
	
	public GSStackVMCompiler(final Node passednode,@Nonnull final String scriptname,final int sourceVersion) {
		super(passednode,scriptname,sourceVersion);
	}
	
	public List<ByteCode> code=new ArrayList<>();
	
	@Override
	public String diagnosticOutput(final State st) {
		final StringBuilder output=new StringBuilder("<pre><table border=0>");
		for (final ByteCode bc: code) {
			final ParseNode bcnode=bc.node();
			output.append("<tr><td>")
			    .append(bcnode!=null?bcnode.tokens():"")
			    .append("</td><td>")
			    .append(bc.explain().replaceFirst(" \\(","</td><td><i>("))
			    .append("</i></td><td>");
			final ArrayList<Byte> bcl=new ArrayList<>();
			bc.toByteCode(bcl);
			for (final Byte b: bcl) {
				output.append(b).append(" ");
			}
			output.append("</td></tr>");
		}
		output.append("</table></pre>");
		return output.toString();
	}
	
	// The compiler has a stack (Last In First Out) which it uses to store 'things'
	// we also have a 'script' which is just a list of things, this time including instructions
	@Nonnull
	public void compile(final State st) {
		lastdebuglineno=-1;
		lastdebugcolno=-1;
		code=new ArrayList<>();
		code.add(new BCString(null,scriptname()));
		code.add(new BCDebugSource(null));
		code.addAll(_compile(st,startnode()));
		code.add(new BCInteger(null,0));
		code.add(new BCReturn(null));
	}
	
	@Override
	public int version() {
		return COMPILER_VERSION;
	}
	
	// ----- Internal Instance -----
	
	
	private void addDebug(@Nonnull final List<ByteCode> compiled,@Nonnull final ParseNode node) {
		final Token firsttoken=node.jjtGetFirstToken();
		if (firsttoken!=null) {
			final int lineno=firsttoken.beginLine;
			final int colno=firsttoken.beginColumn;
			if (lineno!=lastdebuglineno||colno!=lastdebugcolno) {
				compiled.add(new BCDebug(node,lineno,colno));
				lastdebuglineno=lineno;
				lastdebugcolno=colno;
			}
		}
	}
	
	
	@Nonnull
	@Override
	protected List<ByteCode> _compile(@Nonnull final State st,@Nonnull final ParseNode node) {
		final List<ByteCode> compiled=new ArrayList<>();
		if (expectedChildren(node)>-1&&node.jjtGetNumChildren()!=expectedChildren(node)) {
			throw new SystemImplementationException(
					node.getClass().getSimpleName()+" had "+node.jjtGetNumChildren()+" children, expected "+
					expectedChildren(node)+" at line "+node.jjtGetFirstToken().beginLine+", column "+
					node.jjtGetFirstToken().beginColumn);
		}
		
		
		if (node instanceof GSStart||node instanceof GSExpression||node instanceof GSParameter||node instanceof GSTerm||
		    node instanceof GSStatement) { // expression
			// just breaks down into 1 of X executable subtypes
			// Start expands to a list of Statement (types)
			for (int i=0;i<node.jjtGetNumChildren();i++) {
				compiled.addAll(_compile(st,node.child(i)));
			}
			return compiled;
		}
		
		if (node instanceof GSInitialiser) {
			// 3 children node, type, name, value
			checkType(node,0,GSTypeSpecifier.class);
			checkType(node,1,GSIdentifier.class);
			checkType(node,2,GSExpression.class);
			boolean typed=false;
			final String type=node.child(0).tokens();
			// INITIALISE the variable - reverse place name and null content.  Then we just implement "set variable".
			if ("String".equals(type)) {
				compiled.add(new BCString(node));
				typed=true;
			}
			if ("Response".equals(type)) {
				compiled.add(new BCResponse(node));
				typed=true;
			}
			if ("Integer".equals(type)) {
				compiled.add(new BCInteger(node));
				typed=true;
			}
			if ("Float".equals(type)) {
				compiled.add(new BCFloat(node));
				typed=true;
			}
			if ("Avatar".equals(type)) {
				compiled.add(new BCAvatar(node));
				typed=true;
			}
			if ("Group".equals(type)) {
				compiled.add(new BCGroup(node));
				typed=true;
			}
			if ("Character".equals(type)) {
				compiled.add(new BCCharacter(node));
				typed=true;
			}
			if ("List".equals(type)) {
				compiled.add(new BCList(node));
				typed=true;
			}
			if (!typed) {
				throw new SystemImplementationException(
						"Unable to initialise variable of type "+type+" (not implemented)");
			}
			final BCString identifier=new BCString(node.child(1),node.child(1).tokens());
			compiled.add(identifier);
			addDebug(compiled,node);
			compiled.add(new BCInitialise(node));
			// variable initialised.  Assign variable wants to pop the name then content, so content first
			compiled.addAll(_compile(st,node.child(2)));
			compiled.add(identifier);
			addDebug(compiled,node);
			compiled.add(new BCStore(node));
			return compiled;
		}
		if (node instanceof GSAssignment) { // similar to end of GSInitialiser code
			checkType(node,0,GSIdentifierOrList.class);
			checkType(node,1,GSExpression.class);
			final ParseNode target=node.child(0).child(0);
			// this may be a NORMAL VARIABLE (GSIdentifier) or a SPECIFIC LIST ELEMENT (GSIdentifierWithIndex)
			if (target.getClass().equals(GSIdentifier.class)) {
				// assign/varname/value
				compiled.addAll(_compile(st,node.child(1)));
				compiled.add(new BCString(node.child(0),node.child(0).tokens()));
				addDebug(compiled,node);
				compiled.add(new BCStore(node));
				return compiled;
			}
			if (target.getClass().equals(GSIdentifierWithIndex.class)) {
				//assignelement/varname/elementno/value
				compiled.addAll(_compile(st,node.child(1))); // the value
				compiled.addAll(_compile(st,target.child(1))); // evaluate index onto stack
				compiled.add(new BCString(target.child(0),target.child(0).tokens())); // push name
				addDebug(compiled,node);
				compiled.add(new BCStoreIndexed(node));
				return compiled;
			}
			throw new SystemImplementationException(
					"Compiler error: Unknown type of Assignment: "+target.getClass().getName());
		}
		
		if (node instanceof GSStringConstant) {
			String string=node.tokens();
			string=string.substring(1,string.length()-1);
			compiled.add(new BCString(node,string));
			return compiled;
		}
		
		if (node instanceof GSIntegerConstant) {
			compiled.add(new BCInteger(node,node.tokens()));
			return compiled;
		}
		
		if (node instanceof GSFloatConstant) {
			compiled.add(new BCFloat(node,node.tokens()));
			return compiled;
		}
		
		if (node instanceof GSConditional) {
			// conditional branch
			checkType(node,0,GSExpression.class);
			checkType(node,1,GSStatement.class);
			// evaluate the condition, branch if zero, otherwise run the statement
			compiled.addAll(_compile(st,node.child(0))); // compile condition
			final BCLabel jumpsource=new BCLabel(node,jumpnumber);
			jumpnumber++;
			final BCLabel posttruth=new BCLabel(node,jumpnumber);
			jumpnumber++;
			final BCLabel prefalse=new BCLabel(node,jumpnumber);
			jumpnumber++;
			final BCLabel postfalse=new BCLabel(node,jumpnumber);
			jumpnumber++;
			addDebug(compiled,node);
			compiled.add(jumpsource);
			compiled.add(new BCBranchRelativeIfZero(node,jumpsource,posttruth)); // if false, branch to posttruth
			compiled.addAll(_compile(st,node.child(1))); // truth
			compiled.add(new BCInteger(node,0)); // if we're still here (in truth)
			compiled.add(prefalse);
			compiled.add(new BCBranchRelativeIfZero(node,prefalse,postfalse)); // branch to end of whole statement
			compiled.add(posttruth); // where we go if false
			if (node.children()==3) {
				compiled.addAll(_compile(st,node.child(2)));
			}
			compiled.add(postfalse); // end of the whole thing
			return compiled;
		}
		
		if (node instanceof GSWhileLoop) {
			checkType(node,0,GSExpression.class);
			checkType(node,1,GSStatement.class);
			final BCLabel start=new BCLabel(node,jumpnumber);
			jumpnumber++;
			final BCLabel conditionstart=new BCLabel(node,jumpnumber);
			jumpnumber++;
			final BCLabel loopend=new BCLabel(node,jumpnumber);
			jumpnumber++;
			final BCLabel end=new BCLabel(node,jumpnumber);
			jumpnumber++;
			// check condition, exit if false, code block, repeat
			addDebug(compiled,node);
			compiled.add(start);
			compiled.addAll(_compile(st,node.child(0)));
			compiled.add(conditionstart);
			compiled.add(new BCBranchRelativeIfZero(node,conditionstart,end));
			compiled.addAll(_compile(st,node.child(1)));
			compiled.add(new BCInteger(node,0));
			compiled.add(loopend);
			compiled.add(new BCBranchRelativeIfZero(node,loopend,start));
			compiled.add(end);
			return compiled;
		}
		
		if (node instanceof GSFunctionCall) {
			// lots of random glue lives in here, but actually the function call at this level is easy enough, it has a name and some parameters
			checkType(node,0,GSFunctionName.class);
			if (node.jjtGetNumChildren()>1) {
				checkType(node,1,GSParameters.class);
			}
			// validate the function name
			final String functionname=node.child(0).tokens();
			/*if (!validFunction(functionname)) {
				throw new GSUnknownIdentifier("Function "+functionname+" does not exist");
			}*/ // no longer relevant as this code formation is used to call other scripts
			if (priviledgedFunction(functionname)&&!st.hasPermission("Scripting.CompilePrivileged")) {
				throw new UserInputStateException("You can not call function "+functionname+
				                                  " due to not having the permission Scripting.CompilePrivileged");
			}
			// dump the paramters, in reverse order, (which starts with the paramter count), and finally the name and the invoking bytecode
			if (node.jjtGetNumChildren()>1) {
				compiled.addAll(_compile(st,node.child(1)));
			} // assuming it has parameters
			else {
				compiled.add(new BCInteger(node,0));
			} // else zero parameters
			compiled.add(new BCString(node,(node.child(0).tokens())));
			addDebug(compiled,node);
			compiled.add(new BCInvoke(node));
			return compiled;
		}
		
		if (node instanceof GSParameters) {
			for (int i=node.jjtGetNumChildren()-1;i>=0;i--) {
				compiled.addAll(_compile(st,node.child(i)));
			}
			compiled.add(new BCInteger(node,node.children()));
			return compiled;
		}
		
		if (node instanceof GSLogicalOr) {
			compiled.addAll(_compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(_compile(st,node.child(i)));
				compiled.add(new BCOr(node));
			}
			return compiled;
		}
		
		if (node instanceof GSLogicalAnd) {
			compiled.addAll(_compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(_compile(st,node.child(i)));
				compiled.add(new BCAnd(node));
			}
			return compiled;
		}
		
		if (node instanceof GSInEquality) {
			compiled.addAll(_compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(_compile(st,node.child(i)));
				compiled.add(new BCInequality(node));
			}
			return compiled;
		}
		
		if (node instanceof GSEquality) {
			compiled.addAll(_compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(_compile(st,node.child(i)));
				compiled.add(new BCEquality(node));
			}
			return compiled;
		}
		
		if (node instanceof GSLessThan) {
			compiled.addAll(_compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(_compile(st,node.child(i)));
				compiled.add(new BCLessThan2(node));
			}
			return compiled;
		}
		
		if (node instanceof GSGreaterThan) {
			compiled.addAll(_compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(_compile(st,node.child(i)));
				compiled.add(new BCGreaterThan2(node));
			}
			return compiled;
		}
		
		if (node instanceof GSLessOrEqualThan) {
			compiled.addAll(_compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(_compile(st,node.child(i)));
				compiled.add(new BCLessThanEqual2(node));
			}
			return compiled;
		}
		
		if (node instanceof GSGreaterOrEqualThan) {
			compiled.addAll(_compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(_compile(st,node.child(i)));
				compiled.add(new BCGreaterThanEqual2(node));
			}
			return compiled;
		}
		
		if (node instanceof GSAdd) {
			compiled.addAll(_compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(_compile(st,node.child(i)));
				compiled.add(new BCAdd2(node));
			}
			return compiled;
		}
		
		if (node instanceof GSSubtract) {
			compiled.addAll(_compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(_compile(st,node.child(i)));
				compiled.add(new BCSubtract2(node));
			}
			return compiled;
		}
		
		if (node instanceof GSMultiply) {
			compiled.addAll(_compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(_compile(st,node.child(i)));
				compiled.add(new BCMultiply(node));
			}
			return compiled;
		}
		
		if (node instanceof GSDivide) {
			compiled.addAll(_compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(_compile(st,node.child(i)));
				compiled.add(new BCDivide2(node));
			}
			return compiled;
		}
		
		if (node instanceof GSLogicalNot) {
			compiled.addAll(_compile(st,node.child(0)));
			compiled.add(new BCNot(node));
			return compiled;
		}
		
		if (node instanceof GSUnaryMinus) {
			compiled.addAll(_compile(st,node.child(0)));
			compiled.add(new BCNegate(node));
			return compiled;
		}
		
		if (node instanceof GSIdentifier) {
			// pull the variable onto the stack.  Kinda
			compiled.add(new BCString(node,node.tokens()));
			addDebug(compiled,node);
			compiled.add(new BCLoad(node));
			return compiled;
		}
		
		if (node instanceof GSList) {
			// pop the list, in reverse order, then short the size, and then the command.
			for (int i=node.children()-1;i>=0;i--) {
				compiled.addAll(_compile(st,node.child(i)));
			}
			compiled.add(new BCList(node,node.children()));
			return compiled;
		}
		
		if (node instanceof GSListIndex) { // a list index in an evaluatable position
			checkType(node,0,GSIdentifier.class);
			checkType(node,1,GSExpression.class);
			// pop name, pop index
			compiled.addAll(_compile(st,node.child(1)));
			compiled.add(new BCString(node.child(0),node.child(0).tokens()));
			addDebug(compiled,node);
			compiled.add(new BCLoadIndexed(node));
			return compiled;
		}
		
		if (node instanceof GSReturn) { // a return statement with an optional value, otherwise we insert int 0
			if (node.children()>1) {
				throw new GSInternalError("Compilation error, 0 or 1 children expected");
			}
			addDebug(compiled,node);
			if (node.children()==1) {
				compiled.addAll(_compile(st,node.child(0)));
			} else {
				compiled.add(new BCInteger(node,0));
			}
			compiled.add(new BCReturn(node));
			return compiled;
		}
		
		if (node instanceof GSDiscardExpression) { // a top level expression where the result is not assigned and should be binned from the stack
			if (node.children()!=1) {
				throw new GSInternalError("Compilation error, 1 children expected");
			}
			addDebug(compiled,node);
			compiled.addAll(_compile(st,node.child(0)));
			compiled.add(new BCDiscard(node));
			return compiled;
		}
		
		throw new SystemImplementationException(
				"Compilation not implemented for node type '"+node.getClass().getSimpleName()+"'");
	}
	
	private boolean validFunction(final String name) {
		for (final String funname: GSFunctions.getAll().keySet()) {
			if (funname.equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	public Byte[] toByteCode(final State st) {
		if (code.isEmpty()) { compile(st); }
		// twopass
		List<Byte> bytecode=new ArrayList<>();
		for (final ByteCode bc: code) {
			bc.toByteCode(bytecode);
		}
		// redo. now that forward references are completed
		bytecode=new ArrayList<>();
		for (final ByteCode bc: code) {
			bc.toByteCode(bytecode);
		}
		return bytecode.toArray(new Byte[] {});
	}
}
