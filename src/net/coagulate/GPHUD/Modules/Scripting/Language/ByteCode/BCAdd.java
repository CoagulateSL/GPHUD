package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCAdd extends ByteCode {
	public BCAdd(ParseNode n) {
		super(n);
	}

	@Nonnull
	@Override
	public String explain() {
		return "Add (Pop two from stack, add, push result)";
	}
	// Pop two, op, push result

	public void toByteCode(@Nonnull List<Byte> bytes) {
		bytes.add(InstructionSet.Add.get());
	}

	@Override
	public void execute(State st, @Nonnull GSVM vm, boolean simulation) {
		// add the next two stack elements and push the result.
		ByteCodeDataType var1=vm.pop();
		ByteCodeDataType var2=vm.pop();
		// well now, rather depends on their types, some combinations aren't even valid... so have fun with THIS blob
		vm.push(var1.add(var2));
	}

}
