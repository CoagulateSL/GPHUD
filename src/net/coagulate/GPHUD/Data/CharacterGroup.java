package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Database.TooMuchDataException;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.CacheConfig;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reference to a character group.
 * A group of characters within an instance, can represent factions, classes or other groupings.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class CharacterGroup extends TableRow {
	
	private static final Cache<Instance,List<CharacterGroup>> instanceGroupsCache=
			Cache.getCache("gphud/instancegroups",CacheConfig.PERMANENT_CONFIG);
	
	// ---------- STATICS ----------
	
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
	                                @Nonnull final String prefix,
	                                @Nonnull final Attribute a) {
		json.put(prefix+"type","SELECT");
		json.put(prefix+"name","value");
		int count=0;
		for (final CharacterGroup cg: st.getInstance().getGroupsForKeyword(a.getSubType())) {
			if (cg.isOpen()) {
				json.put(prefix+"button"+count,cg.getName());
				count++;
			}
		}
	}
	private static final Cache<Char,List<CharacterGroup>> characterGroupsCache=
			Cache.getCache("GPHUD/characterGroups",CacheConfig.OPERATIONAL_CONFIG);
	private static final Cache<CharacterGroup,Instance> instanceCache=
			Cache.getCache("GPHUD/characterGroupInstance",CacheConfig.PERMANENT_CONFIG);
	private static final Cache<CharacterGroup,Set<Char>> groupMembershipCache=
			Cache.getCache("GPHUD/characterGroupMembers",CacheConfig.OPERATIONAL_CONFIG);
	private static final Cache<CharacterGroup,String> typeCache=
			Cache.getCache("GPHUD/characterGroupType",CacheConfig.OPERATIONAL_CONFIG);
	private static final Cache<CharacterGroup,Integer> precedenceCache=
			Cache.getCache("GPHUD/characterGroupPrecedence",CacheConfig.OPERATIONAL_CONFIG);
	
	/**
	 * Get group by name
	 *
	 * @param st   State
	 * @param name Name of group
	 * @return CharacterGroup or null if not found
	 */
	@Nullable
	public static CharacterGroup resolve(@Nonnull final State st,@Nonnull final String name) {
		return st.getInstance().characterGroupResolveCache.get(name,()->{
			final int id=new CharacterGroup(-1).resolveToID(st,name,true);
			if (id==0) {
				return null;
			}
			return get(id);
		});
	}
	
	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A CharacterGroup representation
	 */
	@Nonnull
	public static CharacterGroup get(final int id) {
		return (CharacterGroup)factoryPut("CharacterGroup",id,CharacterGroup::new);
	}
	
	protected CharacterGroup(final int id) {
		super(id);
		if (id!=-1) {
			validate();
		}
	}
	
	/**
	 * Get all the groups this character is in
	 *
	 * @param state State infers The character to get groups for
	 * @return Set of Character Groups
	 */
	@Nonnull
	public static List<CharacterGroup> getCharactersGroups(@Nonnull final State state) {
		return getGroups(state.getCharacter());
	}
	
	/**
	 * Get all the groups this character is in
	 *
	 * @param character The character to get groups for
	 * @return Set of Character Groups
	 */
	@Nonnull
	public static List<CharacterGroup> getGroups(@Nonnull final Char character) {
		return characterGroupsCache.get(character,()->{
			final List<CharacterGroup> ret=new ArrayList<>();
			for (final ResultsRow r: db().dq(
					"select charactergroupmembers.charactergroupid from charactergroupmembers inner join charactergroups on charactergroupmembers.charactergroupid = charactergroups.charactergroupid  where characterid=? order by charactergroups.kvprecedence asc,charactergroups.charactergroupid asc",
					character.getId())) {
				ret.add(CharacterGroup.get(r.getInt()));
			}
			return ret;
		});
	}
	
	// ----- Internal Statics -----
	
	public static List<CharacterGroup> getInstanceGroups(final Instance instance) {
		return instanceGroupsCache.get(instance,()->{
			final List<CharacterGroup> groups=new ArrayList<>();
			for (final ResultsRow r: db().dq(
					"select charactergroupid from charactergroups where instanceid=? order by kvprecedence asc,charactergroupid asc",
					instance.getId())) {
				groups.add(CharacterGroup.get(r.getInt()));
			}
			return groups;
		});
	}
	
	public static void purgeCharacterGroupPrecedenceCaches() {
		precedenceCache.purgeAll();
	}
	
	/**
	 * Get the character group of a given type.
	 *
	 * @param state     infers character
	 * @param grouptype Group type string
	 * @return The CharacterGroup or null
	 */
	@Nullable
	public static CharacterGroup getGroup(@Nonnull final State state,@Nonnull final String grouptype) {
		return getGroup(state.getCharacter(),grouptype);
	}
	
	// ---------- INSTANCE ----------
	
	/**
	 * Joins a character to this group.
	 * Character must not already be in this group, and must not be in a competing group.
	 *
	 * @param character Character to add
	 * @throws UserInputDuplicateValueException if the character is already in this group, or in a group of a conflicting type
	 */
	public void addMember(@Nonnull final Char character) {
		// in group?
		if (character.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Character (group) / Instance mismatch");
		}
		final int exists=dqinn("select count(*) from charactergroupmembers where charactergroupid=? and characterid=?",
		                       getId(),
		                       character.getId());
		if (exists>0) {
			throw new UserInputDuplicateValueException("Character is already a member of group?");
		}
		// in competing group?
		if (getType()!=null) {
			final CharacterGroup competition=CharacterGroup.getGroup(character,getType());
			if (competition!=null) {
				throw new UserInputDuplicateValueException(
						"Unable to join new group, already in a group of type "+getType()+" - "+competition.getName());
			}
			character.groupTypeCache.purge(getType());
		}
		d("insert into charactergroupmembers(charactergroupid,characterid) values(?,?)",getId(),character.getId());
		characterGroupsCache.purge(character);
		groupMembershipCache.purge(this);
	}
	
	/**
	 * Returns true if there are open groups of this group type, i.e. character creation can meaningfully select one.
	 *
	 * @param st State
	 * @param a  Group attribute (of a subtype)
	 * @return true if there are open (selectable) groups in this type
	 */
	public static boolean hasChoices(final State st,final Attribute a) {
		for (final CharacterGroup cg: st.getInstance().getGroupsForKeyword(a.getSubType())) {
			if (cg.isOpen()) {
				return true;
			}
		}
		return false;
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
	
	public void validate(@Nonnull final State st) {
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("CharacterGroup / State Instance mismatch");
		}
	}
	
	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}
	
	public boolean isOpen() {
		return getBool("open");
	}
	
	public void setOpen(final boolean b) {
		set("open",b);
	}
	
	/**
	 * Purges a particular K from the charactergroup KV store for all groups.
	 *
	 * @param instance Instance to purge from
	 * @param key      Key
	 */
	static void wipeKV(@Nonnull final Instance instance,@Nonnull final String key) {
		final String kvtable="charactergroupkvstore";
		final String maintable="charactergroups";
		final String idcolumn="charactergroupid";
		db().d("delete from "+kvtable+" using "+kvtable+","+maintable+" where "+kvtable+".k like ? and "+kvtable+"."+
		       idcolumn+"="+maintable+"."+idcolumn+" and "+maintable+".instanceid=?",key,instance.getId());
		kvCache.purgeAll();
	}
	
	public static void create(final Instance instance,final String name,final boolean open,final String keyword) {
		final int count=db().dqiNotNull("select count(*) from charactergroups where instanceid=? and name like ?",
		                                instance.getId(),
		                                name);
		if (count>0) {
			throw new UserInputDuplicateValueException("Failed to create group, already exists.");
		}
		db().d("insert into charactergroups(instanceid,name,open,type) values (?,?,?,?)",
		       instance.getId(),
		       name,
		       open,
		       keyword);
		instanceGroupsCache.purge(instance);
		instance.characterGroupResolveCache.purge(name);
	}
	
	@Nonnull
	@Override
	public String getLinkTarget() {
		return "charactergroup";
	}
	
	@Nonnull
	public String getKVTable() {
		return "charactergroupkvstore";
	}
	
	@Nonnull
	public String getKVIdField() {
		return "charactergroupid";
	}
	
	protected int getNameCacheTime() {
		return 60*60;
	} // character groups are likely to end up renamable
	
	/**
	 * Get the character members of the group
	 *
	 * @return Set of Characters
	 */
	@Nonnull
	public Set<Char> getMembers() {
		return groupMembershipCache.get(this,()->{
			final Set<Char> members=new TreeSet<>();
			final Results results=dq("select characterid from charactergroupmembers where charactergroupid=?",getId());
			for (final ResultsRow r: results) {
				final Char record=Char.get(r.getInt());
				members.add(record);
			}
			return members;
		});
	}
	
	/**
	 * Delete this character group
	 */
	public void delete() {
		final Instance instance=getInstance();
		final String name=getName();
		d("delete from charactergroups where charactergroupid=?",getId());
		groupMembershipCache.purge(this);
		characterGroupsCache.purgeAll();
		kvCache.purge(this);
		typeCache.purge(this);
		instanceGroupsCache.purge(instance);
		instance.characterGroupResolveCache.purge(name);
	}
	
	/**
	 * Gets the instance associated with this region
	 *
	 * @return The Instance object
	 */
	@Nullable
	public Instance getInstance() {
		return instanceCache.get(this,()->Instance.get(getInt("instanceid")));
	}
	
	/**
	 * Get the character group of a given type.
	 *
	 * @param character Character to get the group of
	 * @param grouptype Group type string
	 * @return The CharacterGroup or null
	 */
	@Nullable
	public static CharacterGroup getGroup(@Nonnull final Char character,@Nonnull final String grouptype) {
		return character.groupTypeCache.get(grouptype,()->{
			try {
				final Integer group=db().dqi(
						"select charactergroups.charactergroupid from charactergroups inner join charactergroupmembers on charactergroups.charactergroupid=charactergroupmembers"+
						".charactergroupid where characterid=? and charactergroups.type=?",
						character.getId(),
						grouptype);
				if (group==null) {
					return null;
				}
				return CharacterGroup.get(group);
			} catch (@Nonnull final NoDataException e) {
				return null;
			} catch (final TooMuchDataException e) {
				throw new UserInputDuplicateValueException(
						"Character "+character.getName()+" is in multiple groups of type "+grouptype+
						", please contact your Sim Administration to resolve this issue",e,true);
			}
		});
	}
	
	/**
	 * Returns the type of group for this group.
	 *
	 * @return Group type, or null if not typed
	 */
	@Nullable
	public String getType() {
		return typeCache.get(this,()->dqs("select type from charactergroups where charactergroupid=?",getId()));
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
		final int exists=dqinn("select count(*) from charactergroupmembers where charactergroupid=? and characterid=?",
		                       getId(),
		                       character.getId());
		d("delete from charactergroupmembers where charactergroupid=? and characterid=?",getId(),character.getId());
		if (getType()!=null) {
			character.groupTypeCache.purge(getType());
		}
		characterGroupsCache.purge(character);
		groupMembershipCache.purge(this);
		if (exists==0) {
			throw new UserInputStateException("Character not in group.");
		}
	}
	
	/**
	 * Determine if this states character is an admin of the group
	 *
	 * @return true if admin, else false
	 */
	public boolean isAdmin(@Nonnull final State st) {
		return isAdmin(st.getCharacter());
	}
	
	/**
	 * Determines if this character is an admin of the group.
	 * Group owner is always an admin
	 *
	 * @param character Character to check
	 * @return true if admin, false otherwise
	 */
	public boolean isAdmin(@Nonnull final Char character) {
		if (character.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Character (group) / Instance mismatch");
		}
		if (getOwner()==character) {
			return true;
		}
		try {
			final Integer adminflag=dqi(
					"select isadmin from charactergroupmembers where charactergroupid=? and characterid=?",
					getId(),
					character.getId());
			if (adminflag==null) {
				return false;
			}
			return adminflag==1;
		} catch (@Nonnull final NoDataException e) {
			return false;
		}
	}
	
	/**
	 * Get the owner of this group.
	 *
	 * @return Character that owns the group
	 */
	@Nullable
	public Char getOwner() {
		try {
			final Integer owner=dqi("select owner from charactergroups where charactergroupid=?",getId());
			if (owner==null) {
				return null;
			}
			return Char.get(owner);
		} catch (final NoDataException e) {
			throw new UserInputLookupFailureException("Group ID "+getNameSafe()+"/"+getId()+" no longer exists.",e);
		}
	}
	
	/**
	 * Set the owner of this group.
	 *
	 * @param newowner Character to own the group
	 */
	public void setOwner(@Nullable final Char newowner) {
		if (newowner==null) {
			d("update charactergroups set owner=null where charactergroupid=?",getId());
		} else {
			d("update charactergroups set owner=? where charactergroupid=?",newowner.getId(),getId());
		}
	}
	
	/**
	 * Set admin status on a character in this group
	 *
	 * @param character Character to update
	 * @param admin     true/false admin status
	 */
	public void setAdmin(@Nonnull final Char character,final boolean admin) {
		d("update charactergroupmembers set isadmin=? where charactergroupid=? and characterid=?",
		  admin,
		  getId(),
		  character.getId());
	}
	
	/**
	 * Check a character is a member of this group.
	 *
	 * @param character Character to search
	 * @return true/false
	 */
	public boolean hasMember(@Nonnull final Char character) {
		return getMembers().contains(character);
	}

	/**
	 * Gets groups type, or "" if not a typed group
	 *
	 * @return Group types or "", never null.
	 */
	@Nonnull
	public String getTypeNotNull() {
		final String ret=getType();
		if (ret==null) {
			return "";
		}
		return ret;
	}
	
	public int getKVPrecedence() {
		return precedenceCache.get(this,()->getInt("kvprecedence"));
	}
	
	public void setKVPrecedence(final int newPrecedence) {
		set("kvprecedence",newPrecedence);
		precedenceCache.set(this,newPrecedence);
	}
}
