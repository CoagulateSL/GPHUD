package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;

import java.util.List;

public class BCInitialise extends ByteCode {
	public BCInitialise(ParseNode n) {
		super(n);
	}

	// Initialise a variable.  Pop the name and then the (null) content from the
	// POP the NAME.  POP the (null) content which infers the type.
	public String explain() { return "Initialise (Pop name, pop empty content, initialise variable)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Initialise.get());
	}

	@Override
	public void execute(GSVM vm) {
		String variablename=vm.popString().toString();
		ByteCodeDataType value=vm.pop();
		vm.set(variablename,value);
	}


}
