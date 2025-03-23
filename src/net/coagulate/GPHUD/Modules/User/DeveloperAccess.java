package net.coagulate.GPHUD.Modules.User;

import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;
import net.coagulate.Core.Tools.Tokens;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Config;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;

import javax.annotation.Nonnull;

public class DeveloperAccess {
	// ---------- STATICS ----------
	@Nonnull
	@Commands(description="Enable an account for developer access",
	          context=Context.AVATAR,
	          requiresPermission="User.SuperAdmin",
	          permitScripting=false,
	          permitObject=false,
	          permitExternal=false,
	          permitJSON=false,
	          permitUserWeb=false)
	public static Response enableDeveloper(@Nonnull final State state,
	                                       @Arguments(name="user",
	                                                  description="User to grant developer access to",
	                                                  type=ArgumentType.AVATAR) @Nonnull final User user) {
		if (!state.getAvatar().isSuperAdmin()) {
			throw new UserAccessDeniedException("Caller is not a super admin!");
		}
		if (user.hasDeveloperKey()) {
			return new ErrorResponse("User is already a developer");
		}
		final String developerkey=Tokens.generateToken();
		user.setDeveloperKey(developerkey);
		SL.im(user.getUUID(),
		      "Notice from GPHUD "+(Config.getDevelopment()?"DEVELOPMENT":"Production")+
		      " service\n \nYou have been assigned developer access\nKey: "+developerkey+
		      "\n[https://docs.sl.coagulate.net/external_access_api Please see here for a brief developer reference]");
		return new OKResponse("User "+user+" now has a developer key");
		
	}
	
	@Nonnull
	@Commands(description="Disabled an account for developer access",
	          context=Context.AVATAR,
	          requiresPermission="User.SuperAdmin",
	          permitScripting=false,
	          permitObject=false,
	          permitExternal=false,
	          permitJSON=false,
	          permitUserWeb=false)
	public static Response disableDeveloper(@Nonnull final State state,
	                                        @Arguments(name="user",
	                                                   description="User to revoke developer access from",
	                                                   type=ArgumentType.AVATAR) @Nonnull final User user) {
		if (!state.getAvatar().isSuperAdmin()) {
			throw new UserAccessDeniedException("Caller is not a super admin!");
		}
		if (!user.hasDeveloperKey()) {
			return new ErrorResponse("User is not a developer");
		}
		user.setDeveloperKey(null);
		return new OKResponse("User "+user+" now has no developer key");
	}
	
}
