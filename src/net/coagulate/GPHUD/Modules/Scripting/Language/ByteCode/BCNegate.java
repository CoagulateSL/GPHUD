package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCNegate extends ByteCode {
	public BCNegate(final ParseNode node) {
		super(node);
	}
	
	// ---------- INSTANCE ----------
	// Pop two, op, push result
	@Nonnull
	public String explain() {
		return "Negate (Pop one, negate, push)";
	}
	
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Negate.get());
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSVM vm,final boolean simulation) {
		final BCInteger arg1=vm.popInteger();
		vm.push(new BCInteger(null,-arg1.getContent()));
	}
}
