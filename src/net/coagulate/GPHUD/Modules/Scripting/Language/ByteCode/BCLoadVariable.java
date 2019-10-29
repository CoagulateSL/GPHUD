package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import java.util.List;

public class BCLoadVariable extends ByteCode {

	public String explain() { return "LoadVariable (Pop name, push variable value)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.LoadVariable.get());
	}
}
