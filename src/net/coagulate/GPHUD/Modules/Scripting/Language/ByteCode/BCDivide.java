package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCDivide extends ByteCode {
	// Pop two, op, push result
	public String explain() { return "Divide (Pop two, divide, push result)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Divide.get());
	}
}
