package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCMultiply extends ByteCode {
	public BCMultiply(final ParseNode n) {
		super(n);
	}

	// Pop two, op, push result
	@Nonnull
	public String explain() { return "Multiply (Pop two, multiply, push result)"; }

	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Multiply.get());
	}

	@Override
	public void execute(final State st,
	                    @Nonnull final GSVM vm,
	                    final boolean simulation)
	{
		final ByteCodeDataType arg1=vm.pop();
		final ByteCodeDataType arg2=vm.pop();
		vm.push(arg1.multiply(arg2));
	}
}
