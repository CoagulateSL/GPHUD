package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSArrayIndexOutOfBoundsException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSStackVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCLoadIndexed extends ByteCode {
	
	public BCLoadIndexed(final ParseNode node) {
		super(node);
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	public String explain() {
		return "LoadElement (Pop name, pop index, push variable value)";
	}
	
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.LoadIndexed.get());
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSStackVM vm,final boolean simulation) {
		final String name=vm.popString().getContent();
		final int index=vm.popInteger().getContent();
		final BCList list=vm.getList(name);
		if (index>=list.getContent().size()) {
			throw new GSArrayIndexOutOfBoundsException(
					"List "+name+" is "+list.getContent().size()+" long but requested access to element "+index,true);
		}
		vm.push(list.getContent().get(index));
	}
}
