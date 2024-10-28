package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.ByteCodeDataType;
import net.coagulate.GPHUD.State;
import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.Nonnull;
import java.util.List;

/** A wrapper for the execution of scripts compiled as java code */

public class GSJavaVM extends GSVM {
	public GSJavaVM(final Script init) {
	  throw new NotImplementedException();
	}
	
	@Override
	public void invokeOnExit(final String apiCommand) {
		throw new NotImplementedException();
	}
	
	@Override
	public Response execute(final State st) {
		throw new NotImplementedException();
	}
	
	@Override
	public boolean suspended() {
		throw new NotImplementedException();
	}
	
	@Override
	public void suspend(final State st,@Nonnull final Char respondant) {
		throw new NotImplementedException();
	}
	
	@Override
	public String toHtml() {
		throw new NotImplementedException();
	}
	
	@Override
	public void setReturn(final ByteCodeDataType bcdt) {
		throw new NotImplementedException();
	}
	
	@Override
	public Response resume(final State st) {
		throw new NotImplementedException();
	}
	
	@Override
	public List<GSVMExecutionStep> simulate(@Nonnull final State st) {
		throw new NotImplementedException();
	}
}
