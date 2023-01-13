package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCDiscard extends ByteCode {
	public BCDiscard(final ParseNode node) {
		super(node);
	}
	
	// ---------- INSTANCE ----------
	// Assign a value to a variable
	// POP the NAME.  POP the content.
	@Nonnull
	public String explain() {
		return "Discard (Pop value, ignore it)";
	}
	
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Discard.get());
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSVM vm,final boolean simulation) {
		vm.pop();
	}
	
}
