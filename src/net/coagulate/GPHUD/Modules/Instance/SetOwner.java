package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Avatar;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;
import net.coagulate.Core.Tools.UserException;

/** Superadmin command for debugging and administration.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class SetOwner {
    
    @Commands(context = Context.AVATAR,description = "Transfer ownership of the instance (SUPERADMIN ONLY)",requiresPermission = "instance.owner",permitHUDWeb = false,permitJSON = false,permitUserWeb = false)
    public static Response setOwner(State st,
            @Arguments(description = "New owner for this instance",type = ArgumentType.AVATAR)
                Avatar avatar) {
        if (!st.isSuperUser()) { throw new UserException("Instance transfer may only be performed by a SUPERADMIN"); }
        if (avatar==null) { return new ErrorResponse("Target avatar is null or not found"); }
        Avatar oldowner=st.getInstance().getOwner();
        st.getInstance().setOwner(avatar);
        Audit.audit(st, Audit.OPERATOR.AVATAR, null,null,null, "SET", "INSTANCE OWNER", oldowner.getName() , avatar.getName(), "SuperAdmin transferred instance ownership");
        return new OKResponse("Instance ownership has been set");
    }
}
