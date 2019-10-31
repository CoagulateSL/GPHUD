package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;

import java.util.List;

public class BCStore extends ByteCode {
	public BCStore(ParseNode n) {
		super(n);
	}

	// Assign a value to a variable
	// POP the NAME.  POP the content.
	public String explain() { return "Assign (Pop variable name, pop content, assign)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Store.get());
	}

	@Override
	public void execute(GSVM vm) {
		String variablename = vm.popString().getContent();
		ByteCodeDataType value = vm.pop();
		vm.set(variablename,value);
	}

}
