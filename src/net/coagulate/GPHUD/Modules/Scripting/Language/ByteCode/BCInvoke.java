package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCInvoke extends ByteCode {
	// Invoke a function.  Pop name, arg count, N*arguments
	public String explain() { return "Invoke (pop function name, pop arg count, pop arguments)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Invoke.get());
	}
}
