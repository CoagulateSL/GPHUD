package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSStackVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCNegate extends ByteCode {
	public BCNegate(final ParseNode node) {
		super(node);
	}
	
	// ---------- INSTANCE ----------
	// Pop two, op, push result
	@Nonnull
	public String explain() {
		return "Negate (Pop one, negate, push)";
	}
	
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Negate.get());
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSStackVM vm,final boolean simulation) {
		final ByteCodeDataType arg=vm.pop();
		if (arg instanceof BCInteger) {
			vm.push(new BCInteger(null,-arg.toInteger()));
			return;
		}
		if (arg instanceof BCFloat) {
			vm.push(new BCFloat(null,-arg.toFloat()));
			return;
		}
		throw new GSInternalError(
				"Unable to calculate unary minus of type "+arg.getClass().getSimpleName());
	}
}
