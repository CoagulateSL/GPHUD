package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCInitialise extends ByteCode {
	// Initialise a variable.  Pop the name and then the (null) content from the
	// POP the NAME.  POP the (null) content which infers the type.
	public String explain() { return "Initialise (Pop name, pop empty content, initialise variable)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Initialise.get());
	}
}
