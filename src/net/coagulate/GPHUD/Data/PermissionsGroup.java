package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reference to a permissions group.
 * A group of avatars with permissions bound to an instance.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class PermissionsGroup extends TableRow {

	protected PermissionsGroup(int id) { super(id); }

	@Nullable
	public static PermissionsGroup resolve(@Nonnull State st, String v) {
		int id = new PermissionsGroup(-1).resolveToID(st, v, true);
		if (id == 0) { return null; }
		return get(id);
	}

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A Region representation
	 */
	@Nonnull
	public static PermissionsGroup get(int id) {
		return (PermissionsGroup) factoryPut("PermissionsGroup", id, new PermissionsGroup(id));
	}

	/**
	 * Find a region by name.
	 *
	 * @param name Name of region to locate
	 * @return Region object for that region, or null if none is found.
	 */
	@Nullable
	public static PermissionsGroup find(String name, @Nonnull Instance i) {
		try {
			Integer id = GPHUD.getDB().dqi("select permissionsgroupid from permissionsgroups where name like ? and instanceid=?", name, i.getId());
			return get(id);
		} catch (NoDataException e) { return null; }
	}

	@Nonnull
	@Override
	public String getLinkTarget() { return "permissionsgroups"; }

	/**
	 * Gets the instance associated with this region
	 *
	 * @return The Instance object
	 */
	@Nullable
	public Instance getInstance() {
		return Instance.get(getIntNullable("instanceid"));
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "permissionsgroups";
	}

	@Nonnull
	@Override
	public String getIdField() {
		return "permissionsgroupid";
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}


	/**
	 * Get all permissions assigned to this permissions group.
	 *
	 * @param st State
	 * @return Set of String permissions
	 */
	public Set<String> getPermissions(@Nonnull State st) {
		if (!st.permissionsGroupCache.containsKey(getId())) {

			Set<String> permissions = new TreeSet<>();
			Results results = dq("select permission from permissions where permissionsgroupid=?", getId());
			for (ResultsRow r : results) {
				try {
					//Modules.validatePermission(st,r.getString()); - don't validate, some left over perms might be in the DB
					permissions.add(r.getStringNullable());
				} catch (IllegalArgumentException e) {
					st.logger().warning("Permission exists in database but not in schema - [" + r.getStringNullable() + "]");
				}
			}
			st.permissionsGroupCache.put(getId(), permissions);
		}
		return st.permissionsGroupCache.get(getId());
	}

	/**
	 * Get the members of a permissions group.
	 *
	 * @return A Set of PermissionsGroupMemberships
	 */
	@Nonnull
	public Set<PermissionsGroupMembership> getMembers() {
		Set<PermissionsGroupMembership> members = new HashSet<>();
		Results results = dq("select avatarid,caninvite,cankick from permissionsgroupmembers where permissionsgroupid=?", getId());
		for (ResultsRow r : results) {
			PermissionsGroupMembership record = new PermissionsGroupMembership();
			record.avatar = User.get(r.getIntNullable("avatarid"));
			record.caninvite = false;
			if (r.getIntNullable("caninvite") == 1) { record.caninvite = true; }
			record.cankick = false;
			if (r.getIntNullable("cankick") == 1) { record.cankick = true; }
			members.add(record);
		}
		return members;
	}

	/**
	 * Delete this permissionsgroup
	 */
	public void delete() {
		d("delete from permissionsgroups where permissionsgroupid=?", getId());
	}

	/**
	 * Determine if the current avatar can invite to this permissions group
	 *
	 * @param st State
	 * @return true if the current avatar can invite to this group
	 */
	public boolean canInvite(@Nonnull State st) {
		if (st.hasPermission("instance.permissionsmembers")) { return true; }
		try {
			int inviteflag = dqi( "select caninvite from permissionsgroupmembers where permissionsgroupid=? and avatarid=?", getId(), st.getAvatarNullable().getId());
			if (inviteflag == 1) { return true; }
			return false;
		} catch (@Nonnull NullPointerException | NoDataException e) { return false; }
	}

	/**
	 * Determine if the current avatar can eject from this permissions group
	 *
	 * @param st State
	 * @return true if the current avatar can eject from this group
	 */
	public boolean canEject(@Nonnull State st) {
		if (st.hasPermission("instance.permissionsmembers")) { return true; }
		try {
			int inviteflag = dqi( "select cankick from permissionsgroupmembers where permissionsgroupid=? and avatarid=?", getId(), st.getAvatarNullable().getId());
			if (inviteflag == 1) { return true; }
			return false;
		} catch (@Nonnull NullPointerException | NoDataException e) { return false; }
	}

	/**
	 * Add a member to this permissions group
	 *
	 * @param avatar Avatar to add to the group
	 * @throws UserException Avatar can not be added, e.g. is already in group
	 */
	public void addMember(@Nonnull User avatar) throws UserException {
		int exists = dqi( "select count(*) from permissionsgroupmembers where permissionsgroupid=? and avatarid=?", getId(), avatar.getId());
		if (exists > 0) { throw new UserException("Avatar is already a member of group?"); }
		d("insert into permissionsgroupmembers(permissionsgroupid,avatarid) values(?,?)", getId(), avatar.getId());
	}

	/**
	 * Remove a member from this permissions group
	 *
	 * @param avatar Avatar to remove from the group
	 * @throws UserException If the user can not be removed, such as not being in the group
	 */
	public void removeMember(@Nonnull User avatar) throws UserException {
		int exists = dqi( "select count(*) from permissionsgroupmembers where permissionsgroupid=? and avatarid=?", getId(), avatar.getId());
		d("delete from permissionsgroupmembers where permissionsgroupid=? and avatarid=?", getId(), avatar.getId());
		if (exists == 0) { throw new UserException("Avatar not in group."); }
	}

	/**
	 * Set an avatar's permissions over this group
	 *
	 * @param a         Avatar permissions to set
	 * @param caninvite Set the avatar's ability to join other avatars to this group
	 * @param cankick   Set the avatar's ability to remove other avatars from this group
	 * @throws UserException If the user's permissions can not be updated, i.e. not in the group
	 */
	public void setUserPermissions(@Nonnull User a, Boolean caninvite, Boolean cankick) throws UserException {
		int exists = dqi( "select count(*) from permissionsgroupmembers where permissionsgroupid=? and avatarid=?", getId(), a.getId());
		if (exists == 0) { throw new UserException("Avatar not in group."); }
		int inviteval = 0;
		if (caninvite) { inviteval = 1; }
		int kickval = 0;
		if (cankick) { kickval = 1; }
		d("update permissionsgroupmembers set caninvite=?,cankick=? where permissionsgroupid=? and avatarid=?", inviteval, kickval, getId(), a.getId());

	}

	/**
	 * Add a permission to this permissions group.
	 *
	 * @param permission Name of permission to add.
	 * @throws UserException If the permissions is invalid, already added, etc.
	 */
	public void addPermission(State st, @Nonnull String permission) throws UserException {
		Modules.validatePermission(st, permission);
		int exists = dqi( "select count(*) from permissions where permissionsgroupid=? and permission like ?", getId(), permission);
		if (exists != 0) { throw new UserException("Permission already exists on group."); }
		d("insert into permissions(permissionsgroupid,permission) values(?,?)", getId(), permission);
	}

	/**
	 * Remove a permission from this permissions group.
	 *
	 * @param permission Permission to remove
	 * @throws UserException If the permission can not be removed, e.g. is not part of the group.
	 */
	public void removePermission(String permission) throws UserException {
		int exists = dqi( "select count(*) from permissions where permissionsgroupid=? and permission=?", getId(), permission);
		if (exists == 0) { throw new UserException("Permission does not exist in group."); }
		d("delete from permissions where permissionsgroupid=? and permission=?", getId(), permission);
	}

	@Nullable
	public String getKVTable() { return null; }

	@Nullable
	public String getKVIdField() { return null; }

	public void flushKVCache(State st) {}


	public void validate(@Nonnull State st) throws SystemException {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getInstance()) {
			throw new SystemException("PermissionsGroup / State Instance mismatch");
		}
	}

	protected int getNameCacheTime() { return 60 * 60; } // this name doesn't change, cache 1 hour

	public boolean hasPermission(@Nonnull State st, String fullname) {
		for (String permission:getPermissions(st)) {
			if (permission.equalsIgnoreCase(fullname)) { return true; }
		}
		return false;
	}
}
