package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCMultiply extends ByteCode {
	// Pop two, op, push result
	public String explain() { return "BCMultiply (Pop two, multiply, push result)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.BCMultiply.get());
	}
}
