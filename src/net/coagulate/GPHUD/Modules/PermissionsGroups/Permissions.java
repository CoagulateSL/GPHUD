package net.coagulate.GPHUD.Modules.PermissionsGroups;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.PermissionsGroup;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;

/** Add/Remove permissions from a permissions group.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Permissions {
    @URLs(url = "/permissionsgroups/addpermission",requiresPermission = "instance.owner")
    public static void addPermissionForm(State st,SafeMap values) throws UserException, SystemException {
        Modules.simpleHtml(st,"permissionsgroups.addpermission",values);
    }
    
    @Commands(context = Context.AVATAR,description = "Add a permission to a permission group",requiresPermission = "instance.owner")
    public static Response addPermission(State st,
            @Arguments(description = "Permissions group to add permission to",type = ArgumentType.PERMISSIONSGROUP)
                    PermissionsGroup permissionsgroup,
            @Arguments(description = "Permission to add to group",type = ArgumentType.PERMISSION)
                    String permission
            ) throws UserException,SystemException {
        if (!st.isInstanceOwner()) { return new ErrorResponse("No permission to modify the permissions on this group."); }
        Modules.validatePermission(st,permission);
        if (!Modules.getPermission(st,permission).grantable()) { return new ErrorResponse("The permission '"+permission+"' is not grantable through user action."); }
        try { permissionsgroup.addPermission(st,permission); }
        catch (UserException e) { return new ErrorResponse("Failed to add permission to permissions group - "+e.getMessage()); }
        Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "Add Permission", permissionsgroup.getNameSafe(), null, permission, "Permission added to permissions group");
        return new OKResponse("Added "+permission+" to permissions group "+permissionsgroup.getNameSafe());
    }

    @URLs(url = "/permissionsgroups/delpermission",requiresPermission = "instance.owner")
    public static void delPermissionForm(State st,SafeMap values) throws UserException, SystemException {
        Modules.simpleHtml(st,"permissionsgroups.delpermission",values);
    }
    
    @Commands(context = Context.AVATAR,description = "Remove a permission from a permissions group",requiresPermission = "instance.owner")
    public static Response delPermission(State st,
            @Arguments(description = "Permissions group to remove permission from",type = ArgumentType.PERMISSIONSGROUP) 
                    PermissionsGroup permissionsgroup,
            @Arguments(description = "Permission to remove from group",type = ArgumentType.PERMISSION)
                    String permission
    ) throws UserException,SystemException {
        if (!st.isInstanceOwner()) { return new ErrorResponse("No permission to modify the permissions on this group."); }
        try { permissionsgroup.removePermission(permission); }
        catch (UserException e) { return new ErrorResponse("Failed to remove permission from permissions group - "+e.getMessage()); }
        Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "Del Permission", permissionsgroup.getNameSafe(), permission, null, "Permission removed from permissions group");
        return new OKResponse("Removed "+permission+" from permissions group "+permissionsgroup.getNameSafe());
    }
}
