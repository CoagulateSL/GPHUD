package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCDivide2 extends ByteCode {
    public BCDivide2(final ParseNode node) {
        super(node);
    }

    // ---------- INSTANCE ----------
    // Pop two, op, push result
    @Nonnull
    public String explain() {
        return "Divide2 (Pop two, divide deeper by top most, push result)";
    }

    public void toByteCode(@Nonnull final List<Byte> bytes) {
        bytes.add(InstructionSet.Divide2.get());
    }

	@Override
	public void execute(final State st,
	                    @Nonnull final GSVM vm,
	                    final boolean simulation) {
		final ByteCodeDataType arg2=vm.pop();
		final ByteCodeDataType arg1=vm.pop();
		vm.push(arg1.divide(arg2));
	}
}
