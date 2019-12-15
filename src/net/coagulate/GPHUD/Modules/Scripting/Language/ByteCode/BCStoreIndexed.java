package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSArrayIndexOutOfBoundsException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.List;

public class BCStoreIndexed extends ByteCode {
	public BCStoreIndexed(final ParseNode n) {
		super(n);
	}

	// Assign a value to an array index
	// POP the NAME.  POP the index.  POP the content.
	@Nonnull
	public String explain() { return "AssignElement (Pop variable name, pop index, pop content, assign)"; }
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.StoreIndexed.get());
	}

	@Override
	public void execute(final State st, @Nonnull final GSVM vm, final boolean simulation) {
		final BCString variablename=vm.popString();
		final BCInteger index=vm.popInteger();
		final ByteCodeDataType newvalue = vm.pop();
		// get the LIST
		final BCList oldvalue=vm.getList(variablename.getContent());
		// check length
		if (index.getContent()>=oldvalue.getContent().size()) { throw new GSArrayIndexOutOfBoundsException("List "+variablename.getContent()+" is of size "+oldvalue.getContent().size()+" which is <= requested index "+index.getContent()); }
		oldvalue.getContent().set(index.getContent(),newvalue);
		// done
	}

}
