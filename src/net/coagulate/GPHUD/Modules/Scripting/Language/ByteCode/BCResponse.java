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
	String  message="";
	boolean error;
	
	public BCResponse(final ParseNode node) {
		super(node);
	}
	
	public BCResponse(final ParseNode n,@Nonnull final Response content) {
		super(n);
		message=content.scriptResponse();
		if (content instanceof ErrorResponse) {
			error=true;
		}
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	public String explain() {
		return "Response ("+(error?"ERROR:":"")+message+")";
	}
	
	public void toByteCode(@Nonnull final List<Byte> bytes) {
		bytes.add(InstructionSet.Response.get());
		//throw new SystemException("Not implemented");
	}
	
	@Override
	public void execute(final State st,@Nonnull final GSVM vm,final boolean simulation) {
		vm.push(this);
	}
	
	@Nonnull
	@Override
	public BCString toBCString() {
		return new BCString(null,message);
	}
	
	@Nonnull
	public BCInteger toBCInteger() {
		return new BCInteger(null,(error?1:0));
	}
	
	@Nullable
	@Override
	public ByteCodeDataType clone() {
		final BCResponse copy=new BCResponse(null);
		copy.message=message;
		copy.error=error;
		return copy;
	}
	
	public boolean isError() {
		return error;
	}
	
	@Override
	/** Compares the contents, true if equals.  Requires type match, so no auto casting here thanks */
	public boolean strictlyEquals(final ByteCodeDataType find) {
		if (!(find instanceof final BCResponse findr)) {
			return false;
		}
		if (findr.isError()!=isError()) { return false; }
		return findr.getMessage().equals(getMessage());
	}
	
	public String getMessage() { return message; }
}
