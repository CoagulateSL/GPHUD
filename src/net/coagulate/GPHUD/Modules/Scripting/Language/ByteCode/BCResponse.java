package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import java.util.List;

public class BCResponse extends ByteCodeDataType {
	private Response content=null;
	public BCResponse(ParseNode n) {super(n);}
	public BCResponse(ParseNode n,Response content) { super(n); this.content=content; }
	public String explain() { return "Response ("+content+")"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Response.get());
		//throw new SystemException("Not implemented");
	}

	@Override
	public void execute(State st, GSVM vm, boolean simulation) {
		vm.push(this);
	}

	@Override
	public ByteCodeDataType clone() {
		return this;
	}
}
