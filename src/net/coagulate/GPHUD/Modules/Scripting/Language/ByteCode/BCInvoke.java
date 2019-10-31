package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;

import java.util.List;

public class BCInvoke extends ByteCode {
	public BCInvoke(ParseNode n) {
		super(n);
	}

	// Invoke a function.  Pop name, arg count, N*arguments
	public String explain() { return "Invoke (pop function name, pop arg count, pop arguments, push result)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Invoke.get());
	}

	@Override
	public void execute(GSVM vm) {
		String functionname=vm.popString().getContent();
		int argcount=vm.popInteger().getContent();
		ByteCodeDataType args[]=new ByteCodeDataType[argcount];
		for (int i=0;i<argcount;i++) { args[i]=vm.pop(); }
		// MAGIC GOES HERE
		vm.push(new BCString(node(),"NICE!"));
	}
}
