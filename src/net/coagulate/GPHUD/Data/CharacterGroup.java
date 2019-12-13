package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reference to a character group.
 * A group of characters within an instance, can represent factions, classes or other groupings.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class CharacterGroup extends TableRow {

	protected CharacterGroup(int id) { super(id); }

	static void wipeKV(@Nonnull Instance instance, String key) {
		String kvtable = "charactergroupkvstore";
		String maintable = "charactergroups";
		String idcolumn = "charactergroupid";
		GPHUD.getDB().d("delete from " + kvtable + " using " + kvtable + "," + maintable + " where " + kvtable + ".k like ? and " + kvtable + "." + idcolumn + "=" + maintable + "." + idcolumn + " and " + maintable + ".instanceid=?", key, instance.getId());
	}

	@Nonnull
	public static JSONObject createChoice(State st, Attribute a) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	// open groups only
	public static void createChoice(@Nonnull State st, @Nonnull JSONObject json, String prefix, @Nonnull Attribute a) {
		final boolean debug = false;
		json.put(prefix + "type", "SELECT");
		json.put(prefix + "name", "value");
		int count = 0;
		for (CharacterGroup cg : st.getInstance().getGroupsForKeyword(a.getSubType())) {
			//System.out.println("Scanning CG "+cg.getNameSafe());
			if (cg.isOpen()) {
				json.put(prefix + "button" + count, cg.getName());
				count++;
			}
		}
	}

	/**
	 * Get group by name
	 *
	 * @param st   State
	 * @param name Name of group
	 * @return CharacterGroup
	 */
	@Nullable
	public static CharacterGroup resolve(@Nonnull State st, String name) {
		int id = new CharacterGroup(-1).resolveToID(st, name, true);
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
	public static CharacterGroup get(int id) {
		return (CharacterGroup) factoryPut("CharacterGroup", id, new CharacterGroup(id));
	}

	/**
	 * Find a character group by name.
	 *
	 * @param name Name of character group to locate
	 * @return Character group, or null if not found
	 */
	@Nullable
	public static CharacterGroup find(String name, @Nonnull Instance i) {
		try {
			Integer id = GPHUD.getDB().dqi( "select charactergroupid from charactergroups where name like ? and instanceid=?", name, i.getId());
			if (id==null) { return null; }
			return get(id);
		} catch (NoDataException e) { return null; }
	}

	@Nonnull
	@Override
	public String getLinkTarget() { return "charactergroup"; }

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
		return "charactergroups";
	}

	@Nonnull
	@Override
	public String getIdField() {
		return "charactergroupid";
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}

	/**
	 * Get the members of the group
	 *
	 * @return Set of Characters
	 */
	@Nonnull
	public Set<Char> getMembers() {
		Set<Char> members = new TreeSet<>();
		Results results = dq("select characterid from charactergroupmembers where charactergroupid=?", getId());
		for (ResultsRow r : results) {
			Char record = Char.get(r.getInt());
			members.add(record);
		}
		return members;
	}

	/**
	 * Delete this character group
	 */
	public void delete() {
		d("delete from charactergroups where charactergroupid=?", getId());
	}

	/**
	 * Returns the type of group for this group.
	 *
	 * @return Group type
	 */
	@Nullable
	public String getType() { return dqs( "select type from charactergroups where charactergroupid=?", getId()); }


	/**
	 * Joins a character to this group.
	 * Character must not already be in this group, and must not be in a competing group.
	 *
	 * @param character Character to add
	 */
	public void addMember(@Nonnull Char character) {
		// in group?
		if (character.getInstance() != getInstance()) {
			throw new SystemException("Character (group) / Instance mismatch");
		}
		int exists = dqinn( "select count(*) from charactergroupmembers where charactergroupid=? and characterid=?", getId(), character.getId());
		if (exists > 0) { throw new UserException("Character is already a member of group?"); }
		// in competing group?
		CharacterGroup competition = character.getGroup(this.getType());
		if (competition != null) {
			throw new UserException("Unable to join new group, already in a group of type " + this.getType() + " - " + competition.getName());
		}
		d("insert into charactergroupmembers(charactergroupid,characterid) values(?,?)", getId(), character.getId());
	}

	/**
	 * Remove character from this group
	 *
	 * @param character Character to remove
	 */
	public void removeMember(@Nonnull Char character) {
		if (character.getInstance() != getInstance()) {
			throw new SystemException("Character (group) / Instance mismatch");
		}
		int exists = dqinn( "select count(*) from charactergroupmembers where charactergroupid=? and characterid=?", getId(), character.getId());
		d("delete from charactergroupmembers where charactergroupid=? and characterid=?", getId(), character.getId());
		if (exists == 0) { throw new UserException("Character not in group."); }
	}

	/**
	 * Get the owner of this group.
	 *
	 * @return Character that owns the group
	 */
	@Nullable
	public Char getOwner() {
		Integer owner = dqi( "select owner from charactergroups where charactergroupid=?", getId());
		if (owner == null) { return null; }
		return Char.get(owner);
	}

	/**
	 * Set the owner of this group.
	 *
	 * @param newowner Character to own the group
	 */
	public void setOwner(@Nullable Char newowner) {
		if (newowner == null) {
			d("update charactergroups set owner=null where charactergroupid=?", getId());
		} else {
			d("update charactergroups set owner=? where charactergroupid=?", newowner.getId(), getId());
		}
	}

	/**
	 * Determines if this character is an admin of the group.
	 * Group owner is always an admin
	 *
	 * @param character Character to check
	 * @return true if admin, false otherwise
	 */
	public boolean isAdmin(@Nonnull Char character) {
		if (character.getInstance() != getInstance()) {
			throw new SystemException("Character (group) / Instance mismatch");
		}
		if (this.getOwner() == character) { return true; }
		try {
			Integer adminflag = dqi( "select isadmin from charactergroupmembers where charactergroupid=? and characterid=?", getId(), character.getId());
			if (adminflag == null) { return false; }
			if (adminflag == 1) { return true; }
			return false;
		} catch (NoDataException e) { return false; }
	}

	/**
	 * Determine if this states character is an admin of the group
	 *
	 * @return true if admin, else false
	 */
	public boolean isAdmin(@Nonnull State st) { return isAdmin(st.getCharacter()); }

	/**
	 * Set admin status on a character in this group
	 *
	 * @param character Character to update
	 * @param admin     true/false admin status
	 */
	public void setAdmin(@Nonnull Char character, boolean admin) {
		d("update charactergroupmembers set isadmin=? where charactergroupid=? and characterid=?", admin, getId(), character.getId());
	}

	/**
	 * Check a character is a member of this group.
	 *
	 * @param character Character to search
	 * @return true/false
	 */
	public boolean hasMember(@Nonnull Char character) {
		Integer count = dqi( "select count(*) from charactergroupmembers where charactergroupid=? and characterid=?", getId(), character.getId());
		if (count == null) { throw new SystemException("Null response from count statement"); }
		if (count > 1) {
			throw new SystemException("Matched too many members (" + count + ") for " + character + " in CG " + getId() + " - " + getName());
		}
		if (count == 1) { return true; }
		return false;
	}

	@Nonnull
	public String getKVTable() { return "charactergroupkvstore"; }

	@Nonnull
	public String getKVIdField() { return "charactergroupid"; }

	public void validate(@Nonnull State st) throws SystemException {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getInstance()) {
			throw new SystemException("CharacterGroup / State Instance mismatch");
		}
	}

	protected int getNameCacheTime() { return 60; } // character groups are likely to end up renamable

	public Boolean isOpen() {
		return getBool("open");
	}

	public void setOpen(Boolean b) { set("open", b); }

	@Nullable
	public String getTypeNotNull() {
		String ret = getType();
		if (ret == null) { return ""; }
		return ret;
	}

}
