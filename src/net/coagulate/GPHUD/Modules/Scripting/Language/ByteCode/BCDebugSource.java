package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCDebugSource extends ByteCode {

	public BCDebugSource(@Nullable final ParseNode n) {
        super(n);
    }

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String explain() {
		return "Set Debug Source (Pop source script name)";
	}

	@Override
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.DebugSource.get());
	}

	@Override
	public void execute(final State st,
	                    @Nonnull final GSVM vm,
	                    final boolean simulation) {
		vm.source=vm.stack.pop().toBCString().getContent();
	}
}
