package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCSubtract extends ByteCode {
	// Pop two, op, push result
	public String explain() { return "Subtract (Pop two, push one-two)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Subtract.get());
	}
}
