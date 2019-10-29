package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCAssign extends ByteCode {
	// Assign a value to a variable
	// POP the NAME.  POP the content.
	public String explain() { return "Assign (Pop variable name, pop content, assign)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Assign.get());
	}

}
