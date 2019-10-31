package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;

import java.util.List;

public class BCResponse extends ByteCodeDataType {
	private Response content=null;
	public BCResponse() {}
	public BCResponse(Response content) { this.content=content; }
	public String explain() { return "Response("+content+") (push)"; }
	public void toByteCode(List<Byte> bytes) {
		bytes.add(InstructionSet.Response.get());
		//throw new SystemException("Not implemented");
	}

	@Override
	public void execute(GSVM vm) {
		vm.push(this);
	}

	@Override
	public ByteCodeDataType clone() {
		return this;
	}
}
