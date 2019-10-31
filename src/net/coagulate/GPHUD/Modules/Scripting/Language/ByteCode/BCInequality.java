package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;

import java.util.List;

public class BCInequality extends ByteCode {
	// Pop two, op, push result
	public String explain() { return "Inequality (Pop two, push 1 if unequal, 0 if equal)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Inequality.get());
	}

	@Override
	public void execute(GSVM vm) {
		// cheat
		new BCEquality().execute(vm);
		int result=vm.popInteger().toInteger();
		if (result==0) { result=1; } else { result=0; }
		vm.push(new BCInteger(result));
	}
}
