package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSStackVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCGreaterThan2 extends ByteCode {
	public BCGreaterThan2(final ParseNode node) {
		super(node);
	}
	
	// ---------- INSTANCE ----------
	// Pop two, op, push result
	@Nonnull
	public String explain() {
		return "GreaterThan2 (Pop two, compare 2nd pop to 1st, push 1 if greater than, else 0)";
	}
	
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.GreaterThan2.get());
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSStackVM vm,final boolean simulation) {
		final ByteCodeDataType var2=vm.pop();
		final ByteCodeDataType var1=vm.pop();
		//<STRING> | <RESPONSE> | <INT> | <CHARACTER> | <AVATAR> | <GROUP> | "List"
		if (!var1.getClass().equals(var2.getClass())) // no match then
		{
			vm.push(new BCInteger(null,0));
			return;
		}
		final Class<? extends ByteCodeDataType> type=var1.getClass();
		if (type.equals(BCString.class)) {
			final String s1=var1.toString();
			final String s2=var2.toString();
			if (s1.compareTo(s2)>0) {
				vm.push(new BCInteger(null,1));
				return;
			}
			vm.push(new BCInteger(null,0));
			return;
		}
		if (type.equals(BCInteger.class)) {
			final int s1=var1.toInteger();
			final int s2=var2.toInteger();
			if (s1>s2) {
				vm.push(new BCInteger(null,1));
			} else {
				vm.push(new BCInteger(null,0));
			}
			return;
		}
		if (type.equals(BCList.class)) {
			throw new GSInvalidExpressionException("Can not compare lists.  Yet.");
		}
		if (type.equals(BCFloat.class)) {
			final float s1=var1.toFloat();
			final float s2=var2.toFloat();
			if (s1>s2) {
				vm.push(new BCInteger(null,1));
			} else {
				vm.push(new BCInteger(null,0));
			}
			return;
		}
		
		throw new GSInternalError(
				"Unable to calculate greater than between types "+var1.getClass().getSimpleName()+" and "+
				var2.getClass().getSimpleName());
		
	}
}
