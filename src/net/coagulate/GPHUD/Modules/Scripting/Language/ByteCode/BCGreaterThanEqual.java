package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import java.util.List;

public class BCGreaterThanEqual extends ByteCode {
	public BCGreaterThanEqual(ParseNode n) {
		super(n);
	}

	// Pop two, op, push result
	public String explain() { return "GreaterThan (Pop two, compare, push 1 if greater than, else 0)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.GreaterThanEqual.get());
	}

	@Override
	public void execute(State st, GSVM vm, boolean simulation) {
		ByteCodeDataType var1 = vm.pop();
		ByteCodeDataType var2 = vm.pop();
		//<STRING> | <RESPONSE> | <INT> | <CHARACTER> | <AVATAR> | <GROUP> | "List"
		if (!var1.getClass().equals(var2.getClass())) // no match then
		{ vm.push(new BCInteger(null,0)); return; }
		Class type=var1.getClass();
		if (type.equals(BCString.class)) {
			String s1 = var1.toString();
			String s2 = var2.toString();
			if (s1.compareTo(s2)>=0) {
				vm.push(new BCInteger(null,1));
				return;
			}
			vm.push(new BCInteger(null,0));
			return;
		}
		if (type.equals(BCInteger.class)) {
			int s1=var1.toInteger();
			int s2=var2.toInteger();
			if (s1>=s2) { vm.push(new BCInteger(null,1)); } else { vm.push(new BCInteger(null,0)); }
			return;
		}
		if (type.equals(BCList.class)) { throw new GSInvalidExpressionException("Can not compare lists.  Yet."); }

		throw new GSInternalError("Unable to calculate greater than or equal between types "+var1.getClass().getSimpleName()+" and "+var2.getClass().getSimpleName());

	}
}
