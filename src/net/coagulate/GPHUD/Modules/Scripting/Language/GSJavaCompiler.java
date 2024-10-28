package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.*;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.*;
import net.coagulate.GPHUD.State;
import org.apache.commons.lang3.NotImplementedException;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Compile GS code to java bytecode, hopefully safely :P
 *
 * This is an interesting little project ; ultimately I need to be able to pause execution of the compiled class and restore it
 * i.e. the way that scriptruns are archived and restored needs to work ; this was much easier with a custom StackVM implementation
 * where I could just serialise the stack and state and restore that.
 *
 * Now that we're compiling to native java code we either need
 * 1) A persistence - save/restore method for java threads that can be invoked on one
 * or
 * 2) To write the code in a way that supports suspending/resuming without the java VM co-operating
 *
 * While 1 seems kinda nice and perhaps a better solution, there's no obvious way to serialise/deserialise a Java thread
 * and while there may be some third party solutions, the solution to (2) is just a hybridisation of how StackVM works
 *
 * That is, to implement (2) we will largely remove and flatten constructs like if/while and so on, and reimplement them
 * as a series of "branch" commands, with the code simply split up into blocks that can be executed
 *
 * E.G.
 * The statement
 * if (condition) { truth; } else { nottruth; }
 * can be rewritten as
 * if (condition) { goto T; } else { goto F; }
 * T:
 * truth
 * goto E
 * F:
 * false
 * goto E
 * E:
 * rest of program
 *
 * With these blocks numbered we can reduce the program to a "flat" set of code that simply executes a series of blocks
 * and we can suspend/resume at these block markers only.  Specifically
 * a=5
 * a=a+gsHudGetANumber(CALLER)
 * gsSayAsHud(a)
 * becomes
 * a=5
 * gsHudGetANumber(CALLER) -> resume at R:
 * (suspend self)
 * R:
 * a=a+getResult()
 * gsSayAsHud(a)
 *
 * Then all these blocks can just be arranged in a stream of cases in a switch(runBlock) type command.
 * */

public class GSJavaCompiler extends GSCompiler {
	private StringBuilder sb=new StringBuilder();
	private int indent=0;
	public GSJavaCompiler(final GSStart gsscript,final String name) {
		super(gsscript,name);
	}

	@Override
	public List<ByteCode> compile(final State st) {
		sb=new StringBuilder();
		indent=0;
		compile(st,startnode());
		return null;
	}
	
	@Override
	public Byte[] toByteCode(final State st) {
		throw new NotImplementedException();
	}
	
	@Override
	public int version() {
		throw new NotImplementedException();
	}
	
	private void compile(final State st,final ParseNode node) {
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
				compile(st,node.child(i));
				newline(sb,indent);
			}
			return;
		}
		
		if (node instanceof GSInitialiser) {
			// 3 children node, type, name, value
			checkType(node,0,GSTypeSpecifier.class);
			checkType(node,1,GSIdentifier.class);
			checkType(node,2,GSExpression.class);
			final boolean typed=false;
			final String type=node.child(0).tokens();
			// INITIALISE the variable - reverse place name and null content.  Then we just implement "set variable".
			sb.append("vm.putVariable(\"");
			sb.append(node.child(1).tokens());  // identifier
			sb.append("\"");
			sb.append(" = new ");
			sb.append("BC").append(type).append("(null,"); // type
			compile(st,node.child(2));
			sb.append(");");
			return;
		}
		if (node instanceof GSAssignment) { // similar to end of GSInitialiser code
			checkType(node,0,GSIdentifierOrList.class);
			checkType(node,1,GSExpression.class);
			final ParseNode target=node.child(0).child(0);
			// this may be a NORMAL VARIABLE (GSIdentifier) or a SPECIFIC LIST ELEMENT (GSIdentifierWithIndex)
			if (target.getClass().equals(GSIdentifier.class)) {
				// assign/varname/value
				sb.append("vm.putVariable(\"");
				sb.append(node.child(0).tokens()); // identifier
				sb.append("\",");
				compile(st,node.child(1));
				sb.append(";");
				return ;
			}
			if (target.getClass().equals(GSIdentifierWithIndex.class)) {
				//assignelement/varname/elementno/value
				sb.append("((BCList)getVariable(\"");
				sb.append(target.child(0).tokens());
				sb.append("\"))");
				sb.append(".getContent().set(");
				sb.append(Integer.parseInt(target.child(1).tokens()));
				sb.append(",");
				sb.append(node.child(1).tokens());
				sb.append(";");
				return;
			}
			throw new SystemImplementationException(
					"Compiler error: Unknown type of Assignment: "+target.getClass().getName());
		}
		
		if (node instanceof GSStringConstant) {
			// engage paranoia
			String string=node.tokens();
			string=string.substring(1,string.length()-1);
			final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
			sb.append("(");
			sb.append("new String(new byte[]{");
			boolean first=true;
			for (final byte b:bytes) {
				if (first) { first=false; } else { sb.append(", "); }
				sb.append("(byte)");
				sb.append(Byte.toUnsignedInt(b));
			}
			sb.append("})");
			return;
		}
		
		if (node instanceof GSIntegerConstant) {
			sb.append(node.tokens());
			return;
		}
		
		if (node instanceof GSFloatConstant) {
			sb.append(node.tokens());
			return;
		}
		
		if (node instanceof GSConditional) {
			// conditional branch
			checkType(node,0,GSExpression.class);
			checkType(node,1,GSStatement.class);
			// evaluate the condition, branch if zero, otherwise run the statement
			compiled.addAll(compile(st,node.child(0))); // compile condition
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
			compiled.addAll(compile(st,node.child(1))); // truth
			compiled.add(new BCInteger(node,0)); // if we're still here (in truth)
			compiled.add(prefalse);
			compiled.add(new BCBranchRelativeIfZero(node,prefalse,postfalse)); // branch to end of whole statement
			compiled.add(posttruth); // where we go if false
			if (node.children()==3) {
				compiled.addAll(compile(st,node.child(2)));
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
			compiled.addAll(compile(st,node.child(0)));
			compiled.add(conditionstart);
			compiled.add(new BCBranchRelativeIfZero(node,conditionstart,end));
			compiled.addAll(compile(st,node.child(1)));
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
				compiled.addAll(compile(st,node.child(1)));
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
				compiled.addAll(compile(st,node.child(i)));
			}
			compiled.add(new BCInteger(node,node.children()));
			return compiled;
		}
		
		if (node instanceof GSLogicalOr) {
			compiled.addAll(compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(compile(st,node.child(i)));
				compiled.add(new BCOr(node));
			}
			return compiled;
		}
		
		if (node instanceof GSLogicalAnd) {
			compiled.addAll(compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(compile(st,node.child(i)));
				compiled.add(new BCAnd(node));
			}
			return compiled;
		}
		
		if (node instanceof GSInEquality) {
			compiled.addAll(compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(compile(st,node.child(i)));
				compiled.add(new BCInequality(node));
			}
			return compiled;
		}
		
		if (node instanceof GSEquality) {
			compiled.addAll(compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(compile(st,node.child(i)));
				compiled.add(new BCEquality(node));
			}
			return compiled;
		}
		
		if (node instanceof GSLessThan) {
			compiled.addAll(compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(compile(st,node.child(i)));
				compiled.add(new BCLessThan2(node));
			}
			return compiled;
		}
		
		if (node instanceof GSGreaterThan) {
			compiled.addAll(compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(compile(st,node.child(i)));
				compiled.add(new BCGreaterThan2(node));
			}
			return compiled;
		}
		
		if (node instanceof GSLessOrEqualThan) {
			compiled.addAll(compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(compile(st,node.child(i)));
				compiled.add(new BCLessThanEqual2(node));
			}
			return compiled;
		}
		
		if (node instanceof GSGreaterOrEqualThan) {
			compiled.addAll(compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(compile(st,node.child(i)));
				compiled.add(new BCGreaterThanEqual2(node));
			}
			return compiled;
		}
		
		if (node instanceof GSAdd) {
			compiled.addAll(compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(compile(st,node.child(i)));
				compiled.add(new BCAdd2(node));
			}
			return compiled;
		}
		
		if (node instanceof GSSubtract) {
			compiled.addAll(compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(compile(st,node.child(i)));
				compiled.add(new BCSubtract2(node));
			}
			return compiled;
		}
		
		if (node instanceof GSMultiply) {
			compiled.addAll(compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(compile(st,node.child(i)));
				compiled.add(new BCMultiply(node));
			}
			return compiled;
		}
		
		if (node instanceof GSDivide) {
			compiled.addAll(compile(st,node.child(0)));
			for (int i=1;i<node.children();i++) {
				compiled.addAll(compile(st,node.child(i)));
				compiled.add(new BCDivide2(node));
			}
			return compiled;
		}
		
		if (node instanceof GSLogicalNot) {
			compiled.addAll(compile(st,node.child(0)));
			compiled.add(new BCNot(node));
			return compiled;
		}
		
		if (node instanceof GSUnaryMinus) {
			compiled.addAll(compile(st,node.child(0)));
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
				compiled.addAll(compile(st,node.child(i)));
			}
			compiled.add(new BCList(node,node.children()));
			return compiled;
		}
		
		if (node instanceof GSListIndex) { // a list index in an evaluatable position
			checkType(node,0,GSIdentifier.class);
			checkType(node,1,GSExpression.class);
			// pop name, pop index
			compiled.addAll(compile(st,node.child(1)));
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
				compiled.addAll(compile(st,node.child(0)));
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
			compiled.addAll(compile(st,node.child(0)));
			compiled.add(new BCDiscard(node));
			return compiled;
		}
		
		throw new SystemImplementationException(
				"Compilation not implemented for node type '"+node.getClass().getSimpleName()+"'");
	}
	
	private void newline(final StringBuilder sb,final int indent) {
		sb.append("\n");
		for (int i=0;i<indent;i++) { sb.append("\t"); }
	}
}
