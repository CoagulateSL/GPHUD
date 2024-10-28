package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSStackVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCAnd extends ByteCode {
	public BCAnd(final ParseNode node) {
		super(node);
	}
	
	// ---------- INSTANCE ----------
	// Pop two, op, push result
	@Nonnull
	public String explain() {
		return "LogicalAnd (Pop two, if both not zero push 1, else push 0)";
	}
	
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.LogicalAnd.get());
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSStackVM vm,final boolean simulation) {
		final BCInteger arg1=vm.popInteger();
		final BCInteger arg2=vm.popInteger();
		// kinda backwards.  if either is false then...
		if (arg1.getContent()==0||arg2.getContent()==0) {
			vm.push(new BCInteger(null,0));
			return;
		}
		vm.push(new BCInteger(null,1));
	}
}
