package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCStore extends ByteCode {
    public BCStore(final ParseNode node) {
        super(node);
    }

    // ---------- INSTANCE ----------
    // Assign a value to a variable
    // POP the NAME.  POP the content.
    @Nonnull
    public String explain() {
        return "Assign (Pop variable name, pop content, assign)";
    }

    public void toByteCode(@Nonnull final List<Byte> bytes) {
        bytes.add(InstructionSet.Store.get());
	}

	@Override
	public void execute(final State st,
	                    @Nonnull final GSVM vm,
	                    final boolean simulation) {
		final String variablename=vm.popString().getContent();
		final ByteCodeDataType value = vm.pop();
		final ByteCodeDataType existing = vm.get(variablename);
		if (existing==null) { // variable did not already exist
			st.logger().warning("SCRIPTWARNING:"+st.getInstanceNullable()+":"+vm.source+":"+vm.row+":"+vm.column+" - Variable '"+variablename+"' is not defined");
		}
		// Variable type assignment ; e.g. assigning a String to an Integer, is handled in vm.set by some basic polymorphism
		vm.set(variablename,value);
	}

}
