package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Database.TooMuchDataException;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
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

	protected CharacterGroup(final int id) { super(id); }

	/**
	 * Purges a particular K from the charactergroup KV store for all groups.
	 *
	 * @param instance Instance to purge from
	 * @param key      Key
	 */
	static void wipeKV(@Nonnull final Instance instance,
	                   final String key) {
		final String kvtable="charactergroupkvstore";
		final String maintable="charactergroups";
		final String idcolumn="charactergroupid";
		GPHUD.getDB()
		     .d("delete from "+kvtable+" using "+kvtable+","+maintable+" where "+kvtable+".k like ? and "+kvtable+"."+idcolumn+"="+maintable+"."+idcolumn+" and "+maintable+
				        ".instanceid=?",
		        key,
		        instance.getId()
		       );
	}

	/**
	 * Populate a JSON object with options regarding a group.
	 * Sets a SELECT type up.
	 *
	 * @param st     State
	 * @param json   JSON to populate
	 * @param prefix Prefix for the type/name (probably argN)
	 * @param a      Attribute to model the group subtypes off
	 */
	public static void createChoice(@Nonnull final State st,
	                                @Nonnull final JSONObject json,
	                                final String prefix,
	                                @Nonnull final Attribute a) {
		final boolean debug=false;
		json.put(prefix+"type","SELECT");
		json.put(prefix+"name","value");
		int count=0;
		for (final CharacterGroup cg: st.getInstance().getGroupsForKeyword(a.getSubType())) {
			//System.out.println("Scanning CG "+cg.getNameSafe());
			if (cg.isOpen()) {
				json.put(prefix+"button"+count,cg.getName());
				count++;
			}
		}
	}

	/**
	 * Get group by name
	 *
	 * @param st   State
	 * @param name Name of group
	 *
	 * @return CharacterGroup or null if not found
	 */
	@Nullable
	public static CharacterGroup resolve(@Nonnull final State st,
	                                     final String name) {
		final int id=new CharacterGroup(-1).resolveToID(st,name,true);
		if (id==0) { return null; }
		return get(id);
	}

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 *
	 * @return A CharacterGroup representation
	 */
	@Nonnull
	public static CharacterGroup get(final int id) {
		return (CharacterGroup) factoryPut("CharacterGroup",id,new CharacterGroup(id));
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
	public String getIdColumn() {
		return "charactergroupid";
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}

	/**
	 * Get the character members of the group
	 *
	 * @return Set of Characters
	 */
	@Nonnull
	public Set<Char> getMembers() {
		final Set<Char> members=new TreeSet<>();
		final Results results=dq("select characterid from charactergroupmembers where charactergroupid=?",getId());
		for (final ResultsRow r: results) {
			final Char record=Char.get(r.getInt());
			members.add(record);
		}
		return members;
	}

	/**
	 * Delete this character group
	 */
	public void delete() {
		d("delete from charactergroups where charactergroupid=?",getId());
	}

	/**
	 * Returns the type of group for this group.
	 *
	 * @return Group type, or null if not typed
	 */
	@Nullable
	public String getType() { return dqs("select type from charactergroups where charactergroupid=?",getId()); }


	/**
	 * Joins a character to this group.
	 * Character must not already be in this group, and must not be in a competing group.
	 *
	 * @param character Character to add
	 */
	public void addMember(@Nonnull final Char character) {
		// in group?
		if (character.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Character (group) / Instance mismatch");
		}
		final int exists=dqinn("select count(*) from charactergroupmembers where charactergroupid=? and characterid=?",getId(),character.getId());
		if (exists>0) { throw new UserInputDuplicateValueException("Character is already a member of group?"); }
		// in competing group?
		final CharacterGroup competition=character.getGroup(getType());
		if (competition!=null) {
			throw new UserInputDuplicateValueException("Unable to join new group, already in a group of type "+getType()+" - "+competition.getName());
		}
		d("insert into charactergroupmembers(charactergroupid,characterid) values(?,?)",getId(),character.getId());
	}

	/**
	 * Remove character from this group
	 *
	 * @param character Character to remove
	 */
	public void removeMember(@Nonnull final Char character) {
		if (character.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Character (group) / Instance mismatch");
		}
		final int exists=dqinn("select count(*) from charactergroupmembers where charactergroupid=? and characterid=?",getId(),character.getId());
		d("delete from charactergroupmembers where charactergroupid=? and characterid=?",getId(),character.getId());
		if (exists==0) { throw new UserInputStateException("Character not in group."); }
	}

	/**
	 * Get the owner of this group.
	 *
	 * @return Character that owns the group
	 */
	@Nullable
	public Char getOwner() {
		final Integer owner=dqi("select owner from charactergroups where charactergroupid=?",getId());
		if (owner==null) { return null; }
		return Char.get(owner);
	}

	/**
	 * Set the owner of this group.
	 *
	 * @param newowner Character to own the group
	 */
	public void setOwner(@Nullable final Char newowner) {
		if (newowner==null) {
			d("update charactergroups set owner=null where charactergroupid=?",getId());
		}
		else {
			d("update charactergroups set owner=? where charactergroupid=?",newowner.getId(),getId());
		}
	}

	/**
	 * Determines if this character is an admin of the group.
	 * Group owner is always an admin
	 *
	 * @param character Character to check
	 *
	 * @return true if admin, false otherwise
	 */
	public boolean isAdmin(@Nonnull final Char character) {
		if (character.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Character (group) / Instance mismatch");
		}
		if (getOwner()==character) { return true; }
		try {
			final Integer adminflag=dqi("select isadmin from charactergroupmembers where charactergroupid=? and characterid=?",getId(),character.getId());
			if (adminflag==null) { return false; }
			if (adminflag==1) { return true; }
			return false;
		}
		catch (@Nonnull final NoDataException e) { return false; }
	}

	/**
	 * Determine if this states character is an admin of the group
	 *
	 * @return true if admin, else false
	 */
	public boolean isAdmin(@Nonnull final State st) { return isAdmin(st.getCharacter()); }

	/**
	 * Set admin status on a character in this group
	 *
	 * @param character Character to update
	 * @param admin     true/false admin status
	 */
	public void setAdmin(@Nonnull final Char character,
	                     final boolean admin) {
		d("update charactergroupmembers set isadmin=? where charactergroupid=? and characterid=?",admin,getId(),character.getId());
	}

	/**
	 * Check a character is a member of this group.
	 *
	 * @param character Character to search
	 *
	 * @return true/false
	 */
	public boolean hasMember(@Nonnull final Char character) {
		final int count=dqinn("select count(*) from charactergroupmembers where charactergroupid=? and characterid=?",getId(),character.getId());
		if (count>1) {
			throw new TooMuchDataException("Matched too many members ("+count+") for "+character+" in CG "+getId()+" - "+getName());
		}
		if (count==1) { return true; }
		return false;
	}

	@Nonnull
	public String getKVTable() { return "charactergroupkvstore"; }

	@Nonnull
	public String getKVIdField() { return "charactergroupid"; }

	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("CharacterGroup / State Instance mismatch");
		}
	}

	protected int getNameCacheTime() { return 60; } // character groups are likely to end up renamable

	public boolean isOpen() {
		return getBool("open");
	}

	public void setOpen(final boolean b) { set("open",b); }

	/**
	 * Gets groups type, or "" if not a typed group
	 *
	 * @return Group types or "", never null.
	 */
	@Nonnull
	public String getTypeNotNull() {
		final String ret=getType();
		if (ret==null) { return ""; }
		return ret;
	}

}
