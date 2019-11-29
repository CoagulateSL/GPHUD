package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ScriptRuns;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

public class ScriptResponses {

	@Command.Commands(description = "Internal use by scripting engine", permitScripting = false, context = Command.Context.CHARACTER,permitUserWeb = false,permitConsole = false)
	public static Response characterResponse(State st,
	                                         @Argument.Arguments(description="Script PID",type= Argument.ArgumentType.INTEGER)
	                                         Integer processid,
			                                 @Argument.Arguments(description = "The selected character",type = Argument.ArgumentType.CHARACTER)
	                                         Char response) {
		if (response==null) { throw new SystemException("Well, we got a null character for some reason"); }
		if (processid==null) { throw new SystemException("No process id"); }
		ScriptRuns run = ScriptRuns.get(processid);
		if (run.getRespondant()!=st.getCharacter()) { return new ErrorResponse("Script was not expecting a response from you (?)"); }
		GSVM vm=new GSVM(run,st);
		// inject response
		vm.push(new BCCharacter(null,response));
		return vm.resume(st);
	}

	@Command.Commands(description = "Internal use by scripting engine", permitScripting = false, context = Command.Context.CHARACTER,permitUserWeb = false,permitConsole = false)
	public static Response stringResponse(State st,
	                                         @Argument.Arguments(description="Script PID",type= Argument.ArgumentType.INTEGER)
			                                         Integer processid,
	                                         @Argument.Arguments(description = "The string response",type = Argument.ArgumentType.TEXT_ONELINE, max = 1024)
			                                         String response) {
		if (response==null) { throw new SystemException("Well, we got a null string for some reason"); }
		if (processid==null) { throw new SystemException("No process id"); }
		ScriptRuns run = ScriptRuns.get(processid);
		if (run.getRespondant()!=st.getCharacter()) { return new ErrorResponse("Script was not expecting a response from you (?)"); }
		GSVM vm=new GSVM(run,st);
		// inject response
		vm.push(new BCString(null,response));
		return vm.resume(st);
	}
}
