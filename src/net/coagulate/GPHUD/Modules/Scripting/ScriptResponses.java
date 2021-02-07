package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.Core.Database.DBException;
import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.ScriptRun;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCCharacter;
import net.coagulate.GPHUD.Modules.Scripting.Language.ByteCode.BCString;
import net.coagulate.GPHUD.Modules.Scripting.Language.GSVM;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class ScriptResponses {

	// ---------- STATICS ----------
	@Nonnull
	@Command.Commands(description="Internal use by scripting engine",
	                  permitScripting=false,
	                  context=Command.Context.CHARACTER,
	                  permitUserWeb=false,
	                  permitConsole=false,
	                  permitExternal=false,
	                  permitObject=false)
	public static Response characterResponse(@Nonnull final State st,
	                                         @Nonnull @Argument.Arguments(name="processid",description="Script PID",
	                                                                      type=Argument.ArgumentType.INTEGER) final Integer processid,
	                                         @Nonnull @Argument.Arguments(name="response",description="The selected character",
	                                                                      type=Argument.ArgumentType.CHARACTER) final Char response) {
		try {
			final ScriptRun run=ScriptRun.get(processid);
			if (run.getRespondant()!=st.getCharacter()) {
				return new ErrorResponse("Script was not expecting a response from you (?)");
			}
			try { response.validate(st); }
			catch (DBException e) {
				throw new UserInputLookupFailureException("Failed to resolve input to a valid character",e);
			}
			if (response.getInstance()!=st.getInstance()) {
				throw new UserInputLookupFailureException("Failed to resolve input to a valid character at your instance");
			}
			final GSVM vm=new GSVM(run,st);
			// inject response
			vm.push(new BCCharacter(null,response));
			return vm.resume(st);
		} catch (NoDataException e) { throw new UserInputStateException("Your script run has expired or been replaced by a newer script run",true); }
	}

	@Nonnull
	@Command.Commands(description="Internal use by scripting engine",
	                  permitScripting=false,
	                  context=Command.Context.CHARACTER,
	                  permitUserWeb=false,
	                  permitConsole=false,
	                  permitExternal=false,
	                  permitObject=false)
	public static Response stringResponse(@Nonnull final State st,
	                                      @Nonnull @Argument.Arguments(name="processid",description="Script PID",
	                                                                   type=Argument.ArgumentType.INTEGER) final Integer processid,
	                                      @Nonnull @Argument.Arguments(name="response",description="The string response",
	                                                                   type=Argument.ArgumentType.TEXT_ONELINE,
	                                                                   max=1024) final String response) {
		try {
			final ScriptRun run=ScriptRun.get(processid);
			if (run.getRespondant()!=st.getCharacter()) {
				return new ErrorResponse("Script was not expecting a response from you (?)");
			}
			final GSVM vm=new GSVM(run,st);
			// inject response
			vm.push(new BCString(null,response));
			return vm.resume(st);
		} catch (NoDataException e) { throw new UserInputStateException("Your script run has expired or been replaced by a newer script run",true); }
	}
}
