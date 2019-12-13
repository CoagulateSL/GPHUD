package net.coagulate.GPHUD.Modules.Scripting.Language.Functions;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.*;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class API {
	@Nonnull
	@GSFunctions.GSFunction(description = "Calls a standard GPHUD API command",parameters = "Character caller - User invoking the API<br>String apicall - name of API command to call<br>BCList parameters - list of STRING parameters to the target API",returns = "A Response",notes = "")
	public static BCResponse gsAPI(State st, @Nonnull GSVM vm, @Nonnull BCCharacter caller, @Nonnull BCString apicall, @Nonnull BCList parameters) {
		if (vm.simulation) { return new BCResponse(null,new OKResponse("Simulation mode does not call APIs")); }
		try {
			List<String> args = new ArrayList<>();
			for (ByteCodeDataType data : parameters.getContent()) {
				args.add(data.toBCString().getContent());
			}
			State callingstate=new State(caller.getContent());
			callingstate.source= State.Sources.SCRIPTING;
			Response value = Modules.run(callingstate, apicall.getContent(), args);
			return new BCResponse(null, value);
		} catch (RuntimeException e) { throw new SystemException("gsAPI runtimed: "+e.toString(),e); }
	}
}
