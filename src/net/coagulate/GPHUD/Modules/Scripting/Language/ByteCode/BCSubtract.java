package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCSubtract extends ByteCode {
	public BCSubtract(ParseNode n) {
		super(n);
	}

	// Pop two, op, push result
	@Nonnull
	public String explain() { return "Subtract (Pop two, push one-two)"; }
	public void toByteCode(@Nonnull List<Byte> bytes) {
		bytes.add(InstructionSet.Subtract.get());
	}

	@Override
	public void execute(State st, @Nonnull GSVM vm, boolean simulation) {
		ByteCodeDataType arg1 = vm.pop();
		ByteCodeDataType arg2 = vm.pop();
		vm.push(arg1.subtract(arg2));
	}
}
