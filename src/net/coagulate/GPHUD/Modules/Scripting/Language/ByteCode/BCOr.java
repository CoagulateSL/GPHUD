package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCOr extends ByteCode {
    public BCOr(final ParseNode node) {
        super(node);
    }

    // ---------- INSTANCE ----------
    // Pop two, op, push result
    @Nonnull
    public String explain() {
        return "LogicalOr (Pop two, if either not zero push 1, else push 0)";
    }

    public void toByteCode(@Nonnull final List<Byte> bytes) {
        bytes.add(InstructionSet.LogicalOr.get());
    }

	@Override
	public void execute(final State st,
	                    @Nonnull final GSVM vm,
	                    final boolean simulation) {
		final BCInteger arg1=vm.popInteger();
		final BCInteger arg2=vm.popInteger();
		if (arg1.getContent()!=0 || arg2.getContent()!=0) {
			vm.push(new BCInteger(null,1));
			return;
		}
		vm.push(new BCInteger(null,0));
	}
}
