package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.Modules.Scripting.Language.ParseNode;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BCResponse extends ByteCodeDataType {
	String message="";
	boolean error=false;
	public BCResponse(ParseNode n) {super(n);}
	public BCResponse(ParseNode n, @Nonnull Response content) {
		super(n);
		this.message=content.scriptResponse();
		if (content instanceof ErrorResponse) { error=true; }
	}
	@Nonnull
	public String explain() { return "Response ("+(error?"ERROR:":"")+message+")"; }
	public void toByteCode(@Nonnull List<Byte> bytes) {
		bytes.add(InstructionSet.Response.get());
		//throw new SystemException("Not implemented");
	}

	@Override
	public void execute(State st, @Nonnull GSVM vm, boolean simulation) {
		vm.push(this);
	}

	@Nullable
	@Override
	public ByteCodeDataType clone() {
		BCResponse copy=new BCResponse(null);
		copy.message=message;
		copy.error=error;
		return copy;
	}

	@Nullable
	@Override
	public BCString toBCString() {
		return new BCString(null,message);
	}

	@Nullable
	public BCInteger toBCInteger() { return new BCInteger(null,(error?1:0)); }

}
