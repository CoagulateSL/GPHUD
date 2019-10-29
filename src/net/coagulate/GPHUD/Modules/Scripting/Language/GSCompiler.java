package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.*;
import net.coagulate.GPHUD.Modules.Scripting.Language.Generated.*;

import java.util.ArrayList;
import java.util.List;

public class GSCompiler {

	private int jumpnumber=1;
	private int expectedChildren(ParseNode node) {
		if (node instanceof GSStart) { return -1; }
		if (node instanceof GSInitialiser) { return 3; }
		if (node instanceof GSExpression) { return 1; }
		if (node instanceof GSParameter) { return 1; }
		if (node instanceof GSTerm) { return 1; }
		if (node instanceof GSFunctionCall) { return 2; }
		if (node instanceof GSStringConstant) { return 0; }
		if (node instanceof GSIntegerConstant) { return 0; }
		if (node instanceof GSIdentifier) { return 0; }
		if (node instanceof GSParameters) { return -1; }
		if (node instanceof GSConditional) { return 2; }
		if (node instanceof GSAssignment) { return 2; }
		if (node instanceof GSStatement) { return 1; }
		if (node instanceof GSBinaryOperation) { return 3; }
		throw new SystemException("Expected Children not defined for node "+node.getClass().getName());
	}
	private final ParseNode startnode;
	public GSCompiler(Node passednode) {
		if (!(passednode instanceof ParseNode)) { throw new SystemException("Compiler error - passed node is of type "+passednode.getClass().getCanonicalName()+" which is not a ParseNode implementation"); }
		startnode=(ParseNode) passednode;
	}

	public Byte[] toByteCode() {
		List<ByteCode> bytecodelist=compile();
		// twopass
		List<Byte> bytecode=new ArrayList<>();
		for (ByteCode bc:bytecodelist) { bc.toByteCode(bytecode); }
		// redo. now that forward references are completed
		bytecode=new ArrayList<>();
		for (ByteCode bc:bytecodelist) { bc.toByteCode(bytecode); }
		return bytecode.toArray(new Byte[]{});
	}
	// The compiler has a stack (Last In First Out) which it uses to store 'things'
	// we also have a 'script' which is just a list of things, this time including instructions
	public List<ByteCode> compile() { return compile(startnode); }

	private List<ByteCode> compile(ParseNode node) {
		List<ByteCode> compiled=new ArrayList<>();

		if (expectedChildren(node)>-1 && node.jjtGetNumChildren()!=expectedChildren(node)) { throw new SystemException("GSInitialiser had "+node.jjtGetNumChildren()+" children"); }


		if (node instanceof GSStart || node instanceof GSExpression || node instanceof GSParameter || node instanceof GSTerm || node instanceof GSStatement) { // expression just breaks down into 1 of X executable subtypes
			// Start expands to a list of Statement (types)
			for (int i=0;i<node.jjtGetNumChildren();i++) {
				compiled.addAll(compile(node.child(i)));
			}
			return compiled;
		}

		if (node instanceof GSInitialiser) {
			// 3 children node, type, name, value
			checkType(node,0, GSTypeSpecifier.class);
			checkType(node,1, GSIdentifier.class);
			checkType(node,2, GSExpression.class);
			boolean typed=false;
			String type=node.child(0).tokens();
			// INITIALISE the variable - reverse place name and null content.  Then we just implement "set variable".
			if (type.equals("String")) { compiled.add(new BCString()); typed=true; }
			if (type.equals("Response")) { compiled.add(new BCResponse()); typed=true; }
			if (type.equals("Integer")) { compiled.add(new BCInteger()); typed=true; }
			if (type.equals("Avatar")) { compiled.add(new BCAvatar()); typed=true; }
			if (type.equals("Group")) { compiled.add(new BCGroup()); typed=true; }
			if (type.equals("Character")) { compiled.add(new BCCharacter()); typed=true; }
			if (!typed) { throw new SystemException("Unable to initialise variable of type "+type+" (not implemented)"); }
			BCString identifier=new BCString(node.child(1).tokens());
			compiled.add(identifier);
			compiled.add(new BCInitialise());
			// variable initialised.  Assign variable wants to pop the name then content, so content first
			compiled.addAll(compile(node.child(2)));
			compiled.add(identifier);
			compiled.add(new BCAssign());
			return compiled;
		}
		if (node instanceof GSAssignment) { // similar to end of GSInitialiser code
			checkType(node,0,GSIdentifier.class);
			checkType(node,1,GSExpression.class);
			compiled.addAll(compile(node.child(1)));
			compiled.add(new BCString(node.child(0).tokens()));
			compiled.add(new BCAssign());
			return compiled;
		}

		if (node instanceof GSStringConstant) {
			String string=node.tokens();
			string=string.substring(1,string.length()-1);
			compiled.add(new BCString(string));
			return compiled;
		}

		if (node instanceof GSIntegerConstant) {
			compiled.add(new BCInteger(node.tokens()));
			return compiled;
		}


		if (node instanceof GSConditional) {
			// conditional branch
			checkType(node,0,GSExpression.class);
			checkType(node,1,GSStatement.class);
			// evaluate the condition, branch if zero, otherwise run the statement
			compiled.addAll(compile(node.child(0)));
			BCLabel label=new BCLabel(jumpnumber++);
			compiled.add(new BCBranchIfZero(label));
			compiled.addAll(compile(node.child(1)));
			compiled.add(label);
			return compiled;
		}

		if (node instanceof GSFunctionCall) {
			// lots of random glue lives in here, but actually the function call at this level is easy enough, it has a name and some parameters
			checkType(node, 0, GSFunctionName.class);
			checkType(node, 1, GSParameters.class);
			// validate the function name
			String functionname = node.child(0).tokens();
			if (!validFunction(functionname)) {
				throw new UserException("Function " + functionname + " does not exist");
			}
			// dump the paramters, in reverse order, (which starts with the paramter count), and finally the name and the invoking bytecode
			compiled.addAll(compile(node.child(1)));
			compiled.add(new BCString((node.child(0).tokens())));
			compiled.add(new BCInvoke());
			return compiled;
		}

		if (node instanceof GSParameters) {
			for (int i=node.jjtGetNumChildren()-1;i>=0;i--) {
				compiled.addAll(compile(node.child(i)));
			}
			compiled.add(new BCInteger(node.children()));
			return compiled;
		}

		if (node instanceof GSBinaryOperation) {
			// term op expression
			checkType(node,0,GSTerm.class);
			checkType(node,1,GSBinaryOperator.class);
			checkType(node,2,GSExpression.class);
			// exection is just OP pops 2 and pushes result so....
			compiled.addAll(compile(node.child(0)));
			compiled.addAll(compile(node.child(2)));
			boolean handledop=false;
			String op=node.child(1).tokens();
			//"+" | "-" | "*" | "/" | "==" | "!="
			if (op.equals("+")) { handledop=true; compiled.add(new BCAdd()); }
			if (op.equals("-")) { handledop=true; compiled.add(new BCSubtract()); }
			if (op.equals("*")) { handledop=true; compiled.add(new BCMultiply()); }
			if (op.equals("/")) { handledop=true; compiled.add(new BCDivide()); }
			if (op.equals("==")) { handledop=true; compiled.add(new BCEquality()); }
			if (op.equals("!=")) { handledop=true; compiled.add(new BCInequality()); }
			return compiled;
		}

		if (node instanceof GSIdentifier) {
			// pull the variable onto the stack.  Kinda
			compiled.add(new BCString(node.tokens()));
			compiled.add(new BCLoadVariable());
			return compiled;
		}

		throw new SystemException("Compilation not implemented for node type '"+node.getClass().getSimpleName()+"'");
	}

	private void checkType(ParseNode node,int pos,Class clazz) {
		if (node.jjtGetNumChildren()<pos) { throw new SystemException("Checking type failed = has "+node.jjtGetNumChildren()+" and we asked for pos_0: "+pos+" in clazz "+clazz.getName()); }
		Node child=node.jjtGetChild(pos);
		if (!child.getClass().equals(clazz)) { throw new SystemException("Child_0 "+pos+" of "+node.getClass().getName()+" is of type "+child.getClass().getName()+" not the expected "+clazz.getName()); }
	}

	private boolean validFunction(String name) {
		if (name.equals("gsQuery")) { return true; }
		if (name.equals("gsAPI")) { return true; }
		if (name.equals("gsGetKV")) { return true; }
		return false;
	}


}
