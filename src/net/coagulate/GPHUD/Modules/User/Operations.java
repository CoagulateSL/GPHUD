package net.coagulate.GPHUD.Modules.User;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;

/**
 * Set/change a users passwords.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Operations {

	@Commands(context = Context.AVATAR, description = "Set your USER password (via authorised SL login ONLY)", permitUserWeb = false)
	public static Response setPassword(State st,
	                                   @Arguments(description = "New password", type = ArgumentType.PASSWORD)
			                                   String password) throws SystemException, UserException {
		if (st.sourcedeveloper != null && st.sourcedeveloper.getId() != 1) {
			throw new SystemException("RESTRICTED COMMAND");
		}
		if (st.avatar() == null) {
			return new ErrorResponse("Not connected to any user account?  Perhaps you need to register (via User.Register).  Session did not derive a USER context.");
		}
		try { st.avatar().setPassword(password, "Via GPHUD"); } catch (UserException e) {
			return new ErrorResponse(e.getMessage());
		}
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "Replace", "Password", "[CENSORED]", "[CENSORED]", "User set password.");
		return new OKResponse("Password set successfully.");

	}

}
