package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCStore extends ByteCode {
	public BCStore(final ParseNode n) {
		super(n);
	}

	// Assign a value to a variable
	// POP the NAME.  POP the content.
	@Nonnull
	public String explain() { return "Assign (Pop variable name, pop content, assign)"; }

	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Store.get());
	}

	@Override
	public void execute(final State st,
	                    @Nonnull final GSVM vm,
	                    final boolean simulation) {
		final String variablename=vm.popString().getContent();
		final ByteCodeDataType value=vm.pop();
		vm.set(variablename,value);
	}

}
