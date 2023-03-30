package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputEmptyException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.Cache;
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
	
	private static final Cache<Instance,Set<PermissionsGroup>> permissionsGroupsSetCache=
			Cache.getCache("gphud/instancepermissiongroups",CacheConfig.PERMANENT_CONFIG);
	
	// ---------- STATICS ----------
	
	/**
	 * Looks up a permissions group
	 *
	 * @param st   State
	 * @param name Name to lookup
	 * @return Permissions group, or null if not found
	 */
	@Nullable
	public static PermissionsGroup resolveNullable(@Nonnull final State st,@Nonnull final String name) {
		final int id=new PermissionsGroup(-1).resolveToID(st,name,true);
		if (id==0) {
			return null;
		}
		return get(id);
	}
	
	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A Region representation
	 */
	@Nonnull
	public static PermissionsGroup get(final int id) {
		return (PermissionsGroup)factoryPut("PermissionsGroup",id,PermissionsGroup::new);
	}
	
	protected PermissionsGroup(final int id) {
		super(id);
	}
	
	/**
	 * Find a region by name.
	 *
	 * @param name Name of region to locate
	 * @return Region object for that region, or null if none is found.
	 */
	@Nullable
	public static PermissionsGroup find(@Nonnull final String name,@Nonnull final Instance i) {
		return i.permissionsGroupResolverCache.get(name,()->{
			try {
				final int id=db().dqiNotNull(
						"select permissionsgroupid from permissionsgroups where name like ? and instanceid=?",
						name,
						i.getId());
				return get(id);
			} catch (@Nonnull final NoDataException e) {
				return null;
			}
		});
	}
	
	/**
	 * Gets all the permissions a user has at an instance.
	 *
	 * @param instance Instance to look up user/avatar
	 * @param user     User(avatar)
	 * @return Set of permissions.
	 */
	@Nonnull
	public static Set<String> getPermissions(@Nonnull final Instance instance,@Nonnull final User user) {
		final Set<String> permissions=new TreeSet<>();
		final Results results=
				db().dq("select permission from permissions,permissionsgroups,permissionsgroupmembers where permissions.permissionsgroupid=permissionsgroups"+
				        ".permissionsgroupid and instanceid=? and permissionsgroupmembers.permissionsgroupid=permissionsgroups.permissionsgroupid and "+
				        "permissionsgroupmembers.avatarid=?",instance.getId(),user.getId());
		for (final ResultsRow r: results) {
			permissions.add(r.getStringNullable());
		}
		return permissions;
	}
	
	/**
	 * Create a new permissions group in the state's instance
	 *
	 * @param st   State
	 * @param name Permissions group name
	 * @throws UserInputEmptyException          if no name is supplied
	 * @throws UserInputDuplicateValueException if the name is already taken
	 */
	public static void create(@Nonnull final State st,@Nonnull String name) {
		name=name.trim();
		if (name.isEmpty()) {
			throw new UserInputEmptyException("Can not create permissions group with blank name");
		}
		final int exists=db().dqiNotNull("select count(*) from permissionsgroups where name like ? and instanceid=?",
		                                 name,
		                                 st.getInstance().getId());
		if (exists!=0) {
			throw new UserInputDuplicateValueException("Permissions group already exists? ("+exists+" results)");
		}
		db().d("insert into permissionsgroups(name,instanceid) values(?,?)",name,st.getInstance().getId());
		Audit.audit(st,
		            Audit.OPERATOR.AVATAR,
		            null,
		            null,
		            "Create",
		            "PermissionsGroup",
		            null,
		            name,
		            "Avatar created new permissions group");
		permissionsGroupsSetCache.purge(st.getInstance());
		st.getInstance().permissionsGroupResolverCache.purge(name);
	}
	
	/**
	 * Get all the permissionsgroups for an instance.
	 *
	 * @return Set of PermissionsGroups
	 */
	@Nonnull
	public static Set<PermissionsGroup> getPermissionsGroups(@Nonnull final State st) {
		return permissionsGroupsSetCache.get(st.getInstance(),()->{
			final Results results=db().dq("select permissionsgroupid from permissionsgroups where instanceid=?",
			                              st.getInstance().getId());
			final Set<PermissionsGroup> set=new TreeSet<>();
			for (final ResultsRow r: results) {
				set.add(PermissionsGroup.get(r.getInt("permissionsgroupid")));
			}
			return set;
		});
	}
	
	
	// ---------- INSTANCE ----------
	
	/**
	 * Gets the instance associated with this region
	 *
	 * @return The Instance object
	 */
	@Nullable
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}
	
	@Nonnull
	@Override
	public String getTableName() {
		return "permissionsgroups";
	}
	
	@Nonnull
	@Override
	public String getIdColumn() {
		return "permissionsgroupid";
	}
	
	public void validate(@Nonnull final State st) {
		if (validated) {
			return;
		}
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("PermissionsGroup / State Instance mismatch");
		}
	}
	
	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}
	
	@Nonnull
	@Override
	public String getLinkTarget() {
		return "permissionsgroups";
	}
	
	@Nullable
	public String getKVTable() {
		return null;
	}
	
	@Nullable
	public String getKVIdField() {
		return null;
	}
	
	private static final Cache<Integer,Set<PermissionsGroupMembership>> permissionsGroupMembershipCache=Cache.getCache("gphud/permissionsGroupMembershipSet",CacheConfig.DURABLE_CONFIG);
	
	protected int getNameCacheTime() {
		return 60*60;
	} // this name doesn't change, cache 1 hour
	
	/**
	 * Add a member to this permissions group
	 *
	 * @param avatar Avatar to add to the group
	 * @throws UserException Avatar can not be added, e.g. is already in group
	 */
	public void addMember(@Nonnull final User avatar) {
		final int exists=dqinn("select count(*) from permissionsgroupmembers where permissionsgroupid=? and avatarid=?",
		                       getId(),
		                       avatar.getId());
		if (exists>0) {
			throw new UserInputDuplicateValueException("Avatar is already a member of group?");
		}
		d("insert into permissionsgroupmembers(permissionsgroupid,avatarid) values(?,?)",getId(),avatar.getId());
		permissionsGroupMembershipCache.purge(getId());a
	}

	/**
	 * Remove a member from this permissions group
	 *
	 * @param avatar Avatar to remove from the group
	 * @throws UserException If the user can not be removed, such as not being in the group
	 */
	public void removeMember(@Nonnull final User avatar) {
		final int exists=dqinn("select count(*) from permissionsgroupmembers where permissionsgroupid=? and avatarid=?",
		                       getId(),
		                       avatar.getId());
		d("delete from permissionsgroupmembers where permissionsgroupid=? and avatarid=?",getId(),avatar.getId());
		if (exists==0) {
			throw new UserInputStateException("Avatar not in group.");
		}
		permissionsGroupMembershipCache.purge(getId());
	}
	/**
	 * Delete this permissionsgroup
	 */
	public void delete() {
		final Instance instance=getInstance();
		final String name=getName();
		d("delete from permissionsgroups where permissionsgroupid=?",getId());
		permissionsGroupsSetCache.purge(instance);
		getInstance().permissionsGroupResolverCache.purge(name);
	}
	
	/**
	 * Determine if the current avatar can invite to this permissions group
	 *
	 * @param st State
	 * @return true if the current avatar can invite to this group
	 */
	public boolean canInvite(@Nonnull final State st) {
		if (st.hasPermission("instance.permissionsmembers")) {
			return true;
		}
		try {
			final int inviteflag=dqinn(
					"select caninvite from permissionsgroupmembers where permissionsgroupid=? and avatarid=?",
					getId(),
					st.getAvatar().getId());
			return inviteflag==1;
		} catch (@Nonnull final NullPointerException|NoDataException e) {
			return false;
		}
	}
	
	/**
	 * Determine if the current avatar can eject from this permissions group
	 *
	 * @param st State
	 * @return true if the current avatar can eject from this group
	 */
	public boolean canEject(@Nonnull final State st) {
		if (st.hasPermission("instance.permissionsmembers")) {
			return true;
		}
		try {
			final int kickflag=dqinn(
					"select cankick from permissionsgroupmembers where permissionsgroupid=? and avatarid=?",
					getId(),
					st.getAvatar().getId());
			return kickflag==1;
		} catch (@Nonnull final NullPointerException|NoDataException e) {
			return false;
		}
	}
	
	/**
	 * Set an avatar's permissions over this group
	 *
	 * @param user      Avatar permissions to set
	 * @param caninvite Set the avatar's ability to join other avatars to this group
	 * @param cankick   Set the avatar's ability to remove other avatars from this group
	 * @throws UserException If the user's permissions can not be updated, i.e. not in the group
	 */
	public void setUserPermissions(@Nonnull final User user,final boolean caninvite,final boolean cankick) {
		if (!hasMember(user)) {
			throw new UserInputStateException("Avatar not in group.");
		}
		int inviteval=0;
		if (caninvite) {
			inviteval=1;
		}
		int kickval=0;
		if (cankick) {
			kickval=1;
		}
		d("update permissionsgroupmembers set caninvite=?,cankick=? where permissionsgroupid=? and avatarid=?",
		  inviteval,
		  kickval,
		  getId(),
		  user.getId());
		
	}
	
	public boolean hasMember(@Nonnull final User avatar) {
		return getMembers().contains(avatar);
	}
	
	/**
	 * Get the members of a permissions group.
	 *
	 * @return A Set of PermissionsGroupMemberships
	 */
	@Nonnull
	public Set<PermissionsGroupMembership> getMembers() {
		return permissionsGroupMembershipCache.get(getId(),()->{
			final Set<PermissionsGroupMembership> members=new HashSet<>();
			final Results results=
					dq("select avatarid,caninvite,cankick from permissionsgroupmembers where permissionsgroupid=?",getId());
			for (final ResultsRow r: results) {
				final PermissionsGroupMembership record=new PermissionsGroupMembership();
				record.avatar=User.get(r.getInt("avatarid"));
				record.caninvite=r.getInt("caninvite")==1;
				record.cankick=r.getInt("cankick")==1;
				members.add(record);
			}
			return members;
		});
	}
	
	/**
	 * Add a permission to this permissions group.
	 *
	 * @param permission Name of permission to add.
	 * @throws UserException If the permissions is invalid, already added, etc.
	 */
	public void addPermission(@Nonnull final State st,@Nonnull final String permission) {
		Modules.validatePermission(st,permission);
		final int exists=dqinn("select count(*) from permissions where permissionsgroupid=? and permission like ?",
		                       getId(),
		                       permission);
		if (exists!=0) {
			throw new UserInputDuplicateValueException("Permission already exists on group.");
		}
		d("insert into permissions(permissionsgroupid,permission) values(?,?)",getId(),permission);
	}
	
	/**
	 * Remove a permission from this permissions group.
	 *
	 * @param permission Permission to remove
	 * @throws UserException If the permission can not be removed, e.g. is not part of the group.
	 */
	public void removePermission(@Nonnull final String permission) {
		final int exists=dqinn("select count(*) from permissions where permissionsgroupid=? and permission=?",
		                       getId(),
		                       permission);
		if (exists==0) {
			throw new UserInputStateException("Permission does not exist in group.");
		}
		d("delete from permissions where permissionsgroupid=? and permission=?",getId(),permission);
	}
	
	@SuppressWarnings("EmptyMethod") // We have no KVs for permissions groups as they're OOC
	public void flushKVCache(final State st) {
	}
	
	/**
	 * Checks if this group contains a particular permission
	 *
	 * @param st       State
	 * @param fullname Fully qualified permission name
	 * @return True if present, false if not
	 */
	public boolean hasPermission(@Nonnull final State st,@Nonnull final String fullname) {
		for (final String permission: getPermissions(st)) {
			if (permission.equalsIgnoreCase(fullname)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Get all permissions assigned to this permissions group.
	 *
	 * @param st State
	 * @return Set of String permissions
	 */
	@Nonnull
	public Set<String> getPermissions(@Nonnull final State st) {
		if (!st.permissionsGroupCache.containsKey(getId())) {
			
			final Set<String> permissions=new TreeSet<>();
			final Results results=dq("select permission from permissions where permissionsgroupid=?",getId());
			for (final ResultsRow r: results) {
				try {
					//Modules.validatePermission(st,r.getString()); - don't validate, some left over perms might be in the DB
					permissions.add(r.getStringNullable());
				} catch (@Nonnull final IllegalArgumentException e) {
					st.logger()
					  .warning("Permission exists in database but not in schema - ["+r.getStringNullable()+"]");
				}
			}
			st.permissionsGroupCache.put(getId(),permissions);
		}
		return st.permissionsGroupCache.get(getId());
	}
	
	/**
	 * Group membership triplet.
	 */
	public static class PermissionsGroupMembership {
		public User    avatar;
		public boolean caninvite;
		public boolean cankick;
	}
}
