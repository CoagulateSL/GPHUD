package net.coagulate.GPHUD.Modules.User;

import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * Set/change a users passwords.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Operations {

	@Nonnull
	@Commands(context = Context.AVATAR, permitScripting = false, description = "Set your USER password (via authorised SL login ONLY)", permitUserWeb = false,permitObject = false)
	public static Response setPassword(@Nonnull final State st,
	                                   @Nonnull @Arguments(description = "New password", type = ArgumentType.PASSWORD) final
	                                   String password) {
		if (st.getSourcedeveloper().getId() != 1) {
			throw new UserAccessDeniedException("RESTRICTED COMMAND");
		}
		if (st.getAvatarNullable() == null) {
			return new ErrorResponse("Not connected to any user account?  Perhaps you need to register (via User.Register).  Session did not derive a USER context.");
		}
		try { st.getAvatar().setPassword(password, "Via GPHUD"); } catch (@Nonnull final UserException e) {
			return new ErrorResponse(e.getMessage());
		}
		Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "Replace", "Password", "[CENSORED]", "[CENSORED]", "User set password.");
		return new OKResponse("Password set successfully.");

	}

}
