package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInvalidExpressionException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCEquality extends ByteCode {
    public BCEquality(final ParseNode node) {
        super(node);
    }

    // ---------- INSTANCE ----------
    // Pop two, op, push result
    @Nonnull
    public String explain() {
        return "Equality (Pop two, compare, push 1 for match, 0 for no match)";
    }

    public void toByteCode(@Nonnull final List<Byte> bytes) {
        bytes.add(InstructionSet.Equality.get());
    }

	@Override
	public void execute(final State st,
	                    @Nonnull final GSVM vm,
	                    final boolean simulation) {
		final ByteCodeDataType var1=vm.pop();
		final ByteCodeDataType var2=vm.pop();
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
			if (s1.equals(s2)) {
				vm.push(new BCInteger(null,1));
				return;
			}
			vm.push(new BCInteger(null,0));
			return;
		}
		if (type.equals(BCInteger.class)) {
			final int s1=var1.toInteger();
			final int s2=var2.toInteger();
			if (s1==s2) { vm.push(new BCInteger(null,1)); }
			else { vm.push(new BCInteger(null,0)); }
			return;
		}
		if (type.equals(BCCharacter.class)) {
			final int id1=((BCCharacter) var1).getContent().getId();
			final int id2=((BCCharacter) var2).getContent().getId();
			if (id1==id2) { vm.push(new BCInteger(null,1)); }
			else { vm.push(new BCInteger(null,0)); }
			return;
		}
		if (type.equals(BCAvatar.class)) {
			final int id1=((BCAvatar) var1).getContent().getId();
			final int id2=((BCAvatar) var2).getContent().getId();
			if (id1==id2) { vm.push(new BCInteger(null,1)); }
			else { vm.push(new BCInteger(null,0)); }
			return;
		}
		if (type.equals(BCGroup.class)) {
			final int id1=((BCGroup) var1).getContent().getId();
			final int id2=((BCGroup) var2).getContent().getId();
			if (id1==id2) { vm.push(new BCInteger(null,1)); }
			else { vm.push(new BCInteger(null,0)); }
			return;
		}
		if (type.equals(BCList.class)) { throw new GSInvalidExpressionException("Can not compare lists.  Yet."); }

		throw new GSInternalError("Not implemented equality match");

	}
}
