package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import java.util.List;

public class BCMultiply extends ByteCode {
	public BCMultiply(ParseNode n) {
		super(n);
	}

	// Pop two, op, push result
	public String explain() { return "Multiply (Pop two, multiply, push result)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Multiply.get());
	}

	@Override
	public void execute(State st, GSVM vm) {
		ByteCodeDataType arg1 = vm.pop();
		ByteCodeDataType arg2 = vm.pop();
		vm.push(arg1.multiply(arg2));
	}
}
