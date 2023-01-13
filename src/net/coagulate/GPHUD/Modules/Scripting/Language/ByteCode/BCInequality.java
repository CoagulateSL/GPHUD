package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCInequality extends ByteCode {
	public BCInequality(final ParseNode node) {
		super(node);
	}
	
	// ---------- INSTANCE ----------
	// Pop two, op, push result
	@Nonnull
	public String explain() {
		return "Inequality (Pop two, push 1 if unequal, 0 if equal)";
	}
	
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Inequality.get());
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSVM vm,final boolean simulation) {
		// cheat
		new BCEquality(node()).execute(st,vm,true);
		int result=vm.popInteger().toInteger();
		if (result==0) {
			result=1;
		} else {
			result=0;
		}
		vm.push(new BCInteger(node(),result));
	}
}
