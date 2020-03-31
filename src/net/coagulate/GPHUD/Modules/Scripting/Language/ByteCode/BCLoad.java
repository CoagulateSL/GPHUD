package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSUnknownIdentifier;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCLoad extends ByteCode {

	public BCLoad(final ParseNode n) {
		super(n);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	public String explain() { return "LoadVariable (Pop name, push variable value)"; }

	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Load.get());
	}

	@Override
	public void execute(final State st,
	                    @Nonnull final GSVM vm,
	                    final boolean simulation) {
		final String name=vm.popString().getContent();
		final ByteCodeDataType val=vm.get(name);
		if (val==null) { throw new GSUnknownIdentifier("Variable "+name+" is not defined"); }
		vm.push(val);
	}
}
