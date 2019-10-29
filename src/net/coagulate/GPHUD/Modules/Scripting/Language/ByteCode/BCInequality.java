package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCInequality extends ByteCode {
	// Pop two, op, push result
	public String explain() { return "BCInequality (Pop two, push 1 if unequal, 0 if equal)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.BCInequality.get());
	}
}
