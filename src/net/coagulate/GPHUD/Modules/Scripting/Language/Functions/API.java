package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.*;
import net.coagulate.GPHUD.Modules.Scripting.Language.Functions.GSFunctions.SCRIPT_CATEGORY;
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
	@GSFunctions.GSFunction(description="Calls a standard GPHUD API command, throwing a script error if the API call fails",
	                        parameters="Character caller - User invoking the API<br>String apicall - name of API command "+"to"+" call<br>BCList parameters - list of STRING "+"parameters to the target API",
	                        returns="A Response",
	                        notes="",
	                        privileged=false,
	                        category= SCRIPT_CATEGORY.API)
	public static BCResponse gsAPIX(final State st,
	                                @Nonnull final GSVM vm,
	                                @Nonnull final BCCharacter caller,
	                                @Nonnull final BCString apicall,
	                                @Nonnull final BCList parameters) {
		final BCResponse response=gsAPI(st,vm,caller,apicall,parameters,false);
		if (response.isError()) { throw new UserInputStateException(response.toBCString().getContent(),true); }
		return response;
	}

	@Nonnull
	@GSFunctions.GSFunction(description="Calls a standard GPHUD API command with full permissions, throwing a script error if the API call fails",
	                        parameters="Character caller - User invoking the API<br>String apicall - "+"name of API command "+"to"+" call<br>BCList parameters - list of "+"STRING parameters to the target API",
	                        returns="A Response",
	                        notes="",
	                        privileged=true,
	                        category= SCRIPT_CATEGORY.API)
	public static BCResponse gsElevatedAPIX(final State st,
	                                        @Nonnull final GSVM vm,
	                                        @Nonnull final BCCharacter caller,
	                                        @Nonnull final BCString apicall,
	                                        @Nonnull final BCList parameters) {
		final BCResponse response=gsAPI(st,vm,caller,apicall,parameters,true);
		if (response.isError()) { throw new UserInputStateException(response.toBCString().getContent(),true); }
		return response;
	}

	@Nonnull
	@GSFunctions.GSFunction(description="Calls a standard GPHUD API command",
	                        parameters="Character caller - User invoking the API<br>String apicall - name of API command "+"to"+" call<br>BCList parameters - list of STRING "+"parameters to the target API",
	                        returns="A Response",
	                        notes="NOTE: Unless you intend to check the response isn't an error, or truely don't care if it is, it's highly recommended you call gsAPIX to avoid silently discarding errors which may confuse debugging",
	                        privileged=false,
	                        category= SCRIPT_CATEGORY.API)
	public static BCResponse gsAPI(final State st,
	                               @Nonnull final GSVM vm,
	                               @Nonnull final BCCharacter caller,
	                               @Nonnull final BCString apicall,
	                               @Nonnull final BCList parameters) {
		return gsAPI(st,vm,caller,apicall,parameters,false);
	}

	@Nonnull
	@GSFunctions.GSFunction(description="Calls a standard GPHUD API command with full permissions",
	                        parameters="Character caller - User invoking the API<br>String apicall - "+"name of API command "+"to"+" call<br>BCList parameters - list of "+"STRING parameters to the target API",
	                        returns="A Response",
	                        notes="NOTE: Unless you intend to check the response isn't an error, or truely don't care if it is, it's highly recommended you call gsElevatedAPIX to avoid silently discarding errors which may confuse debugging",
	                        privileged=true,
	                        category= SCRIPT_CATEGORY.API)
	public static BCResponse gsElevatedAPI(final State st,
	                                       @Nonnull final GSVM vm,
	                                       @Nonnull final BCCharacter caller,
	                                       @Nonnull final BCString apicall,
	                                       @Nonnull final BCList parameters) {
		return gsAPI(st,vm,caller,apicall,parameters,true);
	}

	// ----- Internal Statics -----
	private static BCResponse gsAPI(@Nonnull final State st,
	                                @Nonnull final GSVM vm,
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
			if (st.getCharacterNullable()==caller.getContent() && vm.getInvokerState()!=null) {
				callingstate.setJson(vm.getInvokerState().jsonNullable());
			}
			if (callingstate.getInstance()!=st.getInstance()) {
				throw new SystemConsistencyException("State instances mismatch in gsAPI, aborting");
			}
			final StringBuilder paramlist=new StringBuilder("(");
			for (final ByteCodeDataType bcdt: parameters.getContent()) {
				if (paramlist.length()>1) { paramlist.append(", "); }
				paramlist.append(bcdt);
			}
			paramlist.append(")");
			//GPHUD.getLogger("gsAPI").fine("gsAPI calling "+apicall+", our state is "+st+" and their state is "+callingstate+", the parameters list is "+paramlist);
			final Response value=Modules.run(callingstate,apicall.getContent(),args);
			return new BCResponse(null,value);
		}
		catch (@Nonnull final UserException e) {
			return new BCResponse(null,new ErrorResponse(e.getLocalizedMessage()));
			//throw new GSExecutionException(apicall+" {"+e.getClass().getSimpleName()+"} "+e.getLocalizedMessage(),e);
		}
		catch (@Nonnull final RuntimeException e) { throw new GSInternalError("gsAPI runtimed: "+e,e); }
	}


}
