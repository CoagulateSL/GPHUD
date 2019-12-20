package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Superadmin command for debugging and administration.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class SetOwner {

	@Nonnull
	@Commands(context=Context.AVATAR, permitScripting=false, description="Transfer ownership of the instance (SUPERADMIN ONLY)", requiresPermission="instance.owner",
	          permitJSON=false, permitUserWeb=false)
	public static Response setOwner(@Nonnull final State st,
	                                @Nullable @Arguments(description="New owner for this instance", type=ArgumentType.AVATAR) final User avatar) {
		if (!st.isSuperUser()) {
			throw new UserAccessDeniedException("Instance transfer may only be performed by a SUPERADMIN");
		}
		if (avatar==null) { return new ErrorResponse("Target avatar is null or not found"); }
		final User oldowner=st.getInstance().getOwner();
		st.getInstance().setOwner(avatar);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"SET","INSTANCE OWNER",oldowner.getName(),avatar.getName(),"SuperAdmin transferred instance ownership");
		return new OKResponse("Instance ownership has been set");
	}
}
