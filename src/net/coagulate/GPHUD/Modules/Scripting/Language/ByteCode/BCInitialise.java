package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCInitialise extends ByteCode {
	public BCInitialise(final ParseNode n) {
		super(n);
	}

	// Initialise a variable.  Pop the name and then the (null) content from the
	// POP the NAME.  POP the (null) content which infers the type.
	@Nonnull
	public String explain() { return "Initialise (Pop name, pop empty content, initialise variable)"; }
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Initialise.get());
	}

	@Override
	public void execute(final State st, @Nonnull final GSVM vm, final boolean simulation) {
		final String variablename=vm.popString().toString();
		final ByteCodeDataType value=vm.pop();
		vm.set(variablename,value);
	}


}
