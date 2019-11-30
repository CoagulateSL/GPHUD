package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Iain Price
 */
public class Permissions {
	/**
	 * Gets all the permissions a user has at an instance.
	 *
	 * @param i Instance to look up user/avatar
	 * @param u User(avatar)
	 * @return Set of permissions.
	 */
	@Nonnull
	public static Set<String> getPermissions(@Nonnull Instance i, @Nonnull User u) {
		Set<String> permissions = new TreeSet<>();
		Results results = GPHUD.getDB().dq("select permission from permissions,permissionsgroups,permissionsgroupmembers where permissions.permissionsgroupid=permissionsgroups.permissionsgroupid and instanceid=? and permissionsgroupmembers.permissionsgroupid=permissionsgroups.permissionsgroupid and permissionsgroupmembers.avatarid=?", i.getId(), u.getId());
		for (ResultsRow r : results) {
			permissions.add(r.getString());
		}
		return permissions;
	}
}
