package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.*;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSInternalError;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class API {
	private API() {}

	// ---------- STATICS ----------
	@Nonnull
	@GSFunctions.GSFunction(description="Calls a standard GPHUD API command",
	                        parameters="Character caller - User invoking the API<br>String apicall - name of API command "+"to"+" call<br>BCList parameters - list of STRING "
			                        +"parameters to the target API",
	                        returns="A Response",
	                        notes="",
	                        privileged=false)
	public static BCResponse gsAPI(final State st,
	                               @Nonnull final GSVM vm,
	                               @Nonnull final BCCharacter caller,
	                               @Nonnull final BCString apicall,
	                               @Nonnull final BCList parameters) {
		return gsAPI(vm,caller,apicall,parameters,false);
	}

	@Nonnull
	@GSFunctions.GSFunction(description="Calls a standard GPHUD API command with full permissions",
	                        parameters="Character caller - User invoking the API<br>String apicall - "+"name of API command "+"to"+" call<br>BCList parameters - list of "+
			                        "STRING parameters to the target API",
	                        returns="A Response",
	                        notes="",
	                        privileged=true)
	public static BCResponse gsElevatedAPI(final State st,
	                                       @Nonnull final GSVM vm,
	                                       @Nonnull final BCCharacter caller,
	                                       @Nonnull final BCString apicall,
	                                       @Nonnull final BCList parameters) {
		return gsAPI(vm,caller,apicall,parameters,true);
	}

	// ----- Internal Statics -----
	private static BCResponse gsAPI(@Nonnull final GSVM vm,
	                                @Nonnull final BCCharacter caller,
	                                @Nonnull final BCString apicall,
	                                @Nonnull final BCList parameters,
	                                final boolean elevated) {
		if (vm.simulation) { return new BCResponse(null,new OKResponse("Simulation mode does not call APIs")); }
		try {
			final List<String> args=new ArrayList<>();
			for (final ByteCodeDataType data: parameters.getContent()) {
				args.add(data.toBCString().getContent());
			}
			final State callingstate=new State(caller.getContent());
			callingstate.fleshOut();
			callingstate.source=State.Sources.SCRIPTING;
			if (elevated) { callingstate.elevate(true); }
			// some things care about this.  like initialise and logon
			if (vm.getInvokerState()!=null && vm.getInvokerState().getCharacterNullable()==caller.getContent()) {
				callingstate.setJson(vm.getInvokerState().jsonNullable());
			}
			final Response value=Modules.run(callingstate,apicall.getContent(),args);
			return new BCResponse(null,value);
		}
		catch (@Nonnull final RuntimeException e) { throw new GSInternalError("gsAPI runtimed: "+e,e); }
	}


}
