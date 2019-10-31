package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Modules.Scripting.Language.GSArrayIndexOutOfBoundsException;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;

import java.util.List;

public class BCAssignElement extends ByteCode {
	// Assign a value to an array index
	// POP the NAME.  POP the index.  POP the content.
	public String explain() { return "AssignElement (Pop variable name, pop index, pop content, assign)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.AssignElement.get());
	}

	@Override
	public void execute(GSVM vm) {
		BCString variablename=vm.popString();
		BCInteger index=vm.popInteger();
		ByteCodeDataType newvalue = vm.pop();
		// get the LIST
		BCList oldvalue=vm.getList(variablename.getContent());
		// check length
		if (index.getContent()>=oldvalue.getContent().size()) { throw new GSArrayIndexOutOfBoundsException("List "+variablename.getContent()+" is of size "+oldvalue.getContent().size()+" which is <= requested index "+index.getContent()); }
		oldvalue.getContent().set(index.getContent(),newvalue);
		// done
	}

}
