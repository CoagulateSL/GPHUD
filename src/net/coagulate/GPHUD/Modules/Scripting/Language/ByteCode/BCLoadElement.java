package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSArrayIndexOutOfBoundsException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;

import java.util.List;

public class BCLoadElement extends ByteCode {

	public String explain() { return "LoadElement (Pop name, pop index, push variable value)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.LoadElement.get());
	}

	@Override
	public void execute(GSVM vm) {
		String name=vm.popString().getContent();
		int index=vm.popInteger().getContent();
		BCList list=vm.getList(name);
		if (index>=list.getContent().size()) {
			throw new GSArrayIndexOutOfBoundsException("List "+name+" is "+list.getContent().size()+" long but requested access to element "+index);
		}
		vm.push(list.getContent().get(index));
	}
}
