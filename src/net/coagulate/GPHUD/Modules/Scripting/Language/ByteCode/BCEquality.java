package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCEquality extends ByteCode {
	// Pop two, op, push result
	public String explain() { return "BCEquality (Pop two, compare, push 1 for match, 0 for no match)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.BCEquality.get());
	}
}
