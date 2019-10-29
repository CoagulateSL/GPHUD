package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCAdd extends ByteCode {
	@Override
	public String explain() {
		return "BCAdd (Pop two from stack, add, push result)";
	}
	// Pop two, op, push result

	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.BCAdd.get());
	}
}
