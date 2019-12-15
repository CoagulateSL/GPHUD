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
	@Nonnull
	@GSFunctions.GSFunction(description = "Calls a standard GPHUD API command",parameters = "Character caller - User invoking the API<br>String apicall - name of API command to call<br>BCList parameters - list of STRING parameters to the target API",returns = "A Response",notes = "")
	public static BCResponse gsAPI(final State st, @Nonnull final GSVM vm, @Nonnull final BCCharacter caller, @Nonnull final BCString apicall, @Nonnull final BCList parameters) {
		if (vm.simulation) { return new BCResponse(null,new OKResponse("Simulation mode does not call APIs")); }
		try {
			final List<String> args = new ArrayList<>();
			for (final ByteCodeDataType data : parameters.getContent()) {
				args.add(data.toBCString().getContent());
			}
			final State callingstate=new State(caller.getContent());
			callingstate.source= State.Sources.SCRIPTING;
			final Response value = Modules.run(callingstate, apicall.getContent(), args);
			return new BCResponse(null, value);
		} catch (final RuntimeException e) { throw new GSInternalError("gsAPI runtimed: "+ e,e); }
	}
}
