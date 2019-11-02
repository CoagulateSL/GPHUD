package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSArrayIndexOutOfBoundsException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import java.util.List;

public class BCLoadIndexed extends ByteCode {

	public BCLoadIndexed(ParseNode n) {
		super(n);
	}

	public String explain() { return "LoadElement (Pop name, pop index, push variable value)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.LoadIndexed.get());
	}

	@Override
	public void execute(State st, GSVM vm, boolean simulation) {
		String name=vm.popString().getContent();
		int index=vm.popInteger().getContent();
		BCList list=vm.getList(name);
		if (index>=list.getContent().size()) {
			throw new GSArrayIndexOutOfBoundsException("List "+name+" is "+list.getContent().size()+" long but requested access to element "+index);
		}
		vm.push(list.getContent().get(index));
	}
}
