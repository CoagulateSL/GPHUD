package net.coagulate.GPHUD.Modules.PermissionsGroups;

import net.coagulate.Core.Exceptions.UserException;
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
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * Add/Remove permissions from a permissions group.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Permissions {
	// ---------- STATICS ----------
	@URLs(url="/permissionsgroups/addpermission",
	      requiresPermission="Instance.ManagePermissions")
	public static void addPermissionForm(@Nonnull final State st,
	                                     @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"permissionsgroups.addpermission",values);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Add a permission to a permission group",
	          requiresPermission="Instance.ManagePermissions")
	public static Response addPermission(@Nonnull final State st,
	                                     @Nonnull
	                                     @Arguments(description="Permissions group to add permission to",
	                                                type=ArgumentType.PERMISSIONSGROUP) final PermissionsGroup permissionsgroup,
	                                     @Nonnull
	                                     @Arguments(description="Permission to add to group",
	                                                type=ArgumentType.PERMISSION) final String permission) {
		Modules.validatePermission(st,permission);
		final Permission permissionref=Modules.getPermission(st,permission);
		if (permissionref==null) {
			return new ErrorResponse("The permission '"+permission+"' did not resolve properly (does not exist?)");
		}
		if (!permissionref.grantable()) {
			return new ErrorResponse("The permission '"+permission+"' is not grantable through user action.");
		}
		try { permissionsgroup.addPermission(st,permission); }
		catch (@Nonnull final UserException e) {
			return new ErrorResponse("Failed to add permission to permissions group - "+e.getMessage());
		}
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Add Permission",permissionsgroup.getNameSafe(),null,permission,"Permission added to permissions group");
		return new OKResponse("Added "+permission+" to permissions group "+permissionsgroup.getNameSafe());
	}

	@URLs(url="/permissionsgroups/delpermission",
	      requiresPermission="Instance.ManagePermissions")
	public static void delPermissionForm(@Nonnull final State st,
	                                     @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"permissionsgroups.delpermission",values);
	}

	@Nonnull
	@Commands(context=Context.AVATAR,
	          description="Remove a permission from a permissions group",
	          requiresPermission="Instance.ManagePermissions")
	public static Response delPermission(@Nonnull final State st,
	                                     @Nonnull
	                                     @Arguments(description="Permissions group to remove permission from",
	                                                type=ArgumentType.PERMISSIONSGROUP) final PermissionsGroup permissionsgroup,
	                                     @Arguments(description="Permission to remove from group",
	                                                type=ArgumentType.TEXT_CLEAN,
	                                                max=256) final String permission) {
		try { permissionsgroup.removePermission(permission); }
		catch (@Nonnull final UserException e) {
			return new ErrorResponse("Failed to remove permission from permissions group - "+e.getMessage());
		}
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Del Permission",permissionsgroup.getNameSafe(),permission,null,"Permission removed from permissions group");
		return new OKResponse("Removed "+permission+" from permissions group "+permissionsgroup.getNameSafe());
	}
}
