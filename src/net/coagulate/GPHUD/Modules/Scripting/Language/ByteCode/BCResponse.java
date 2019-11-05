package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import java.util.List;

public class BCResponse extends ByteCodeDataType {
	String message="";
	boolean error=false;
	public BCResponse(ParseNode n) {super(n);}
	public BCResponse(ParseNode n,Response content) {
		super(n);
		this.message=content.scriptResponse();
		if (content instanceof ErrorResponse) { error=true; }
	}
	public String explain() { return "Response ("+(error?"ERROR:":"")+message+")"; }
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
		BCResponse copy=new BCResponse(null);
		copy.message=message;
		copy.error=error;
		return copy;
	}

	@Override
	public BCString toBCString() {
		return new BCString(null,message);
	}

	public BCInteger toBCInteger() { return new BCInteger(null,(error?1:0)); }

}
