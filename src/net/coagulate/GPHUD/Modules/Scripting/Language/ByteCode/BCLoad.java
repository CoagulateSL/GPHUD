package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSUnknownIdentifier;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import java.util.List;

public class BCLoad extends ByteCode {

	public BCLoad(ParseNode n) {
		super(n);
	}

	public String explain() { return "LoadVariable (Pop name, push variable value)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Load.get());
	}

	@Override
	public void execute(State st, GSVM vm, boolean simulation) {
		String name=vm.popString().getContent();
		ByteCodeDataType val=vm.get(name);
		if (val==null) { throw new GSUnknownIdentifier("Variable "+name+" is not defined"); }
		vm.push(val);
	}
}