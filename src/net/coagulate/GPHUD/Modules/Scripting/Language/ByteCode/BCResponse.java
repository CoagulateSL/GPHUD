package net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode;

import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSStackVM;
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
	
	@Nonnull
	@Override
	public ByteCodeDataType add(@Nonnull final ByteCodeDataType var) {
		throw fail();
	}
	
	@Nonnull
	@Override
	public ByteCodeDataType subtract(@Nonnull final ByteCodeDataType var) {
		throw fail();
	}
	
	@Nonnull
	@Override
	public ByteCodeDataType multiply(@Nonnull final ByteCodeDataType var) {
		throw fail();
	}
	
	@Nonnull
	@Override
	public ByteCodeDataType divide(@Nonnull final ByteCodeDataType var) {
		throw fail();
	}
	
	@Override
	public ByteCodeDataType unaryMinus() {
		throw fail();
	}
	
	@Nonnull
	@Override
	public BCInteger valueEquals(@Nonnull final ByteCodeDataType var) {
		if (var instanceof BCInteger) { return toBoolean(toInteger()==var.toInteger()); }
		if (var instanceof BCString) { return toBoolean(toString().equalsIgnoreCase(((BCString)var).getContent())); }
		throw fail(var);
	}
	
	@Nonnull
	@Override
	public BCInteger lessThan(@Nonnull final ByteCodeDataType var) {
		throw fail();
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
	public void execute(final State st,@Nonnull final GSStackVM vm,final boolean simulation) {
		vm.push(this);
	}
	
	@Nonnull
	@Override
	public BCString toBCString() {
		return new BCString(null,message);
	}
	
	@Nonnull
	@Override
	public BCInteger greaterThan(@Nonnull final ByteCodeDataType var) {
		throw fail();
	}
	
	@Nonnull
	public BCInteger toBCInteger() {
		return new BCInteger(null,(error?1:0));
	}
	
	@Override
	public BCInteger not() {
		throw fail();
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
	/** Compares the contents, true if equals.  Requires type match, so no auto casting here thanks */ public boolean strictlyEquals(
			final ByteCodeDataType find) {
		if (!(find instanceof final BCResponse findr)) {
			return false;
		}
		if (findr.isError()!=isError()) {
			return false;
		}
		return findr.getMessage().equals(getMessage());
	}
	
	public String getMessage() {
		return message;
	}

	@Nonnull
	@Override
	public BCFloat toBCFloat() {
		return new BCFloat(null,error?1.0f:0.0f);
	}
	
	@Override
	public boolean toBoolean() {
		return error;
	}
}
