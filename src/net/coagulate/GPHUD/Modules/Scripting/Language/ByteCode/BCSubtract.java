package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import java.util.List;

public class BCSubtract extends ByteCode {
	public BCSubtract(ParseNode n) {
		super(n);
	}

	// Pop two, op, push result
	public String explain() { return "Subtract (Pop two, push one-two)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Subtract.get());
	}

	@Override
	public void execute(State st, GSVM vm, boolean simulation) {
		ByteCodeDataType arg1 = vm.pop();
		ByteCodeDataType arg2 = vm.pop();
		vm.push(arg1.subtract(arg2));
	}
}
