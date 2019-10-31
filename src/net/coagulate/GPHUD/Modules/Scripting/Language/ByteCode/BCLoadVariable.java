package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;

import java.util.List;

public class BCLoadVariable extends ByteCode {

	public String explain() { return "LoadVariable (Pop name, push variable value)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.LoadVariable.get());
	}

	@Override
	public void execute(GSVM vm) {
		String name=vm.popString().getContent();
		ByteCodeDataType val=vm.get(name);
		if (val==null) { throw new GSInvalidExpressionException("Variable "+name+" is not defined"); }
		vm.push(val);
	}
}
