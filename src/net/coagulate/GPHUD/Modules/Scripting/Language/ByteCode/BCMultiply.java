package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCMultiply extends ByteCode {
	public BCMultiply(ParseNode n) {
		super(n);
	}

	// Pop two, op, push result
	@Nonnull
	public String explain() { return "Multiply (Pop two, multiply, push result)"; }
	public void toByteCode(@Nonnull List<Byte> bytes) {
		bytes.add(InstructionSet.Multiply.get());
	}

	@Override
	public void execute(State st, @Nonnull GSVM vm, boolean simulation) {
		ByteCodeDataType arg1 = vm.pop();
		ByteCodeDataType arg2 = vm.pop();
		vm.push(arg1.multiply(arg2));
	}
}
