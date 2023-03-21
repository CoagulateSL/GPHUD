package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemRemoteFailureException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputEmptyException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.User.UserInputValidationFilterException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Interface;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Maintenance;
import net.coagulate.GPHUD.Modules.Experience.Experience;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Pool;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.logging.Level.SEVERE;
import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * Reference to a character within an instance
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Char extends TableRow {
	
	private static final Cache<Char,Instance> instanceCache=
			Cache.getCache("GPHUD/charInstance",CacheConfig.PERMANENT_CONFIG);
	private static final Cache<Char,Integer> protocolCache=
			Cache.getCache("GPHUD/charProtocol",CacheConfig.PERMANENT_CONFIG);
	private static final Cache<Char,Zone> zoneCache=Cache.getCache("gphud/characterZone",CacheConfig.MUTABLE);
	// ---------- STATICS ----------
	protected final Cache<Pool,Integer> poolSumCache=
			Cache.getCache("GPHUD/characterPoolSums/"+getId(),CacheConfig.OPERATIONAL_CONFIG);
	
	/**
	 * Return a set of all characters inside a given zone.
	 *
	 * @param zone The zone to enumerate
	 * @return A set of Char(acters) that are registered inside the zone.
	 */
	@Nonnull
	public static Set<Char> getInZone(@Nonnull final Zone zone) {
		final Set<Char> chars=new TreeSet<>();
		for (final ResultsRow r: db().dq("select characterid from characters where zoneid=? and url is not null",
		                                 zone.getId())) {
			chars.add(Char.get(r.getInt()));
		}
		return chars;
	}
	
	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A Char representation
	 */
	@Nonnull
	public static Char get(final int id) {
		return (Char)factoryPut("Character",id,Char::new);
	}
	
	protected Char(final int id) {
		super(id);
	}
	
	/**
	 * Find character by name
	 *
	 * @param st   State
	 * @param name Character name
	 * @return Character
	 */
	@Nullable
	public static Char resolve(@Nonnull final State st,final String name) {
		final int id=new Char(-1).resolveToID(st,name,true);
		if (id==0) {
			return null;
		}
		return get(id);
	}
	
	@Nullable
	public static Char findNullable(final Instance instance,final String name) {
		final Results results=GPHUD.getDB()
		                           .dq("select characterid from characters where name like ? and instanceid=?",
		                               name,
		                               instance.getId());
		if (results.empty()) {
			return null;
		}
		if (results.size()>1) {
			return null;
		}
		return Char.get(results.first().getInt());
	}
	
	/**
	 * Gets a list of all active characters
	 * Specifically those with inbound URLs
	 *
	 * @param i Instance
	 * @return Set of Characters that have inbound links
	 */
	@Nonnull
	public static Set<Char> getActive(final Instance i) {
		final Set<Char> chars=new TreeSet<>();
		for (final ResultsRow r: db().dq("select characterid from characters where url is not null and instanceid=?",
		                                 i.getId())) {
			chars.add(get(r.getInt()));
		}
		return chars;
	}
	
	/**
	 * Get list of a users non-retired characters at a given instance.
	 *
	 * @param instance The instance
	 * @param avatar   The user to get a list of characters for
	 * @return List of unretired Char (characters)
	 */
	@Nonnull
	public static Set<Char> getCharacters(@Nonnull final Instance instance,@Nonnull final User avatar) {
		final Results rows=db().dq("select characterid from characters where owner=? and retired=0 and instanceid=?",
		                           avatar.getId(),
		                           instance.getId());
		final Set<Char> results=new TreeSet<>();
		for (final ResultsRow r: rows) {
			results.add(Char.get(r.getInt()));
		}
		return results;
	}
	
	/**
	 * Get all characters for the avatar, at any instance.
	 *
	 * @return Set of Char that are owned by this avatar and not retired
	 */
	@Nonnull
	public static Set<Char> getCharacters(@Nonnull final User a) {
		final Results rows=db().dq("select characterid from characters where owner=? and retired=0",a.getId());
		final Set<Char> results=new TreeSet<>();
		for (final ResultsRow r: rows) {
			results.add(Char.get(r.getInt()));
		}
		return results;
	}
	
	public static void create(@Nonnull final State st,@Nonnull final String name,final boolean filter) {
		if (filter) {
			// check some KVs about the name..
			//check Instance.AllowedNamingSymbols
			checkAllowedNamingSymbols(st,name);
			//check Instance.FilteredNamingList
			checkFilteredNamingList(st,name);
		}
		GPHUD.getLogger(st.getInstance().toString()).info("About to create "+name+" in "+st.getInstance());
		db().d("insert into characters(name,instanceid,owner,lastactive,retired) values(?,?,?,?,?)",
		       name,
		       st.getInstance().getId(),
		       st.getAvatar().getId(),
		       0,
		       0);
	}
	
	/**
	 * Get most recent character an avatar used, from any instance
	 *
	 * @param avatar User to look up
	 * @return Most recent character used, possibly null.
	 */
	@Nullable
	public static Char getMostRecent(@Nonnull final User avatar) {
		final Results results=db().dq(
				"select characterid from characters where owner=? and retired=0 order by lastactive desc limit 0,1",
				avatar.getId());
		if (results.empty()) {
			return null;
		}
		try {
			return Char.get(results.iterator().next().getInt("characterid"));
		} catch (@Nonnull final Exception e) { // weird
			GPHUD.getLogger().log(SEVERE,"Exception while instansiating most recently used character?",e);
			return null;
		}
	}
	
	/**
	 * Get most recent character used at an instance
	 *
	 * @param avatar           User to look up most recent character for
	 * @param optionalInstance Specific instance to search in, or null for find in all instances
	 * @return Most recently used character matching parameters, or null
	 */
	@Nullable
	public static Char getMostRecent(@Nonnull final User avatar,@Nullable final Instance optionalInstance) {
		if (optionalInstance==null) {
			return getMostRecent(avatar);
		}
		final Results results=db().dq(
				"select characterid from characters where owner=? and retired=0 and instanceid=? order by lastactive desc limit 0,1",
				avatar.getId(),
				optionalInstance.getId());
		if (results.empty()) {
			return null;
		}
		try {
			return Char.get(results.iterator().next().getInt("characterid"));
		} catch (@Nonnull final Exception e) { // weird
			GPHUD.getLogger().log(SEVERE,"Exception while instansiating most recently used character?",e);
			return null;
		}
	}
	
	/**
	 * Return a HTML list of userid/username of NPCs at this instance
	 *
	 * @param st       Inters instance
	 * @param listName name attribute of the HTML list
	 * @return A DropDownList
	 */
	@Nonnull
	public static DropDownList getNPCList(@Nonnull final State st,final String listName) {
		final DropDownList list=new DropDownList(listName);
		for (final ResultsRow row: db().dq("select characterid,name from characters where instanceid=? and owner=?",
		                                   st.getInstance().getId(),
		                                   User.getSystem().getId())) {
			list.add(String.valueOf(row.getIntNullable("characterid")),row.getStringNullable("name"));
		}
		return list;
	}
	
	/**
	 * Get a list of HUDs that haven't checked in in over 60 seconds
	 * <p>
	 *
	 * @return Results set of a db query (boo)
	 */
	public static Results getPingable() {
		return db().dq(
				"select characterid,name,url,urllast from characters where url is not null and authnode like ? and urllast<? order by urllast asc "+
				"limit 0,30",
				Interface.getNode(),
				getUnixTime()-(Maintenance.PINGHUDINTERVAL*60));
	}
	
	/**
	 * Auto create a default character
	 *
	 * @param st State
	 * @return Freshly created character
	 */
	public static Char autoCreate(@Nonnull final State st) {
		Char.create(st,st.getAvatar().getName(),false); // don't filter avatar based names
		final Char character=getMostRecent(st.getAvatar(),st.getInstance());
		if (character==null) {
			st.logger().severe("Created character for avatar but avatar has no characters still");
			throw new NoDataException("Could not create a character for this avatar");
		}
		Audit.audit(st,
		            Audit.OPERATOR.AVATAR,
		            st.getAvatar(),
		            Char.get(character.getId()),
		            "Create",
		            "Character",
		            null,
		            st.getAvatar().getName(),
		            "Automatically generated character upon login with no characters.");
		return character;
	}
	
	
	/**
	 * Disconnect a URL - that is, log the character out, but don't terminate the URL its self.
	 *
	 * @param url URL to disconnect from existing resources
	 */
	public static void disconnectURL(@Nonnull final String url) {
		// is this URL in use?
		for (final ResultsRow row: db().dq("select characterid from characters where url like ?",url)) {
			final Char character=Char.get(row.getInt());
			Visit.closeVisits(character,character.getRegion());
			character.disconnect();
		}
	}
	
	public static Table statusDump(final State st) {
		final Table t=new Table().border();
		t.header("Character ID");
		t.header("Name");
		t.header("Owner");
		t.header("Player");
		t.header("Last Active (approx)");
		t.header("Retired");
		t.header("URL");
		t.header("URL First Seen");
		t.header("URL Last Seen");
		t.header("Servicing Server");
		t.header("Zone");
		t.header("Region");
		t.header("protocol");
		for (final ResultsRow row: db().dq(
				"select * from characters where instanceid=? and (url is not null or playedby is not null)",
				st.getInstance().getId())) {
			t.openRow();
			t.add(row.getIntNullable("characterid"));
			t.add(row.getStringNullable("name"));
			final Integer owner=row.getIntNullable("owner");
			t.add(owner==null?"Null?":User.get(owner).getName()+"[#"+owner+"]");
			final Integer playedBy=row.getIntNullable("playedby");
			t.add(playedBy==null?"":User.get(playedBy).getName()+"[#"+playedBy+"]");
			t.add(UnixTime.fromUnixTime(row.getIntNullable("lastactive"),st.getAvatar().getTimeZone()));
			Integer retired=row.getIntNullable("retired");
			if (retired==null) {
				retired=0;
			}
			t.add(retired==0?"":"Retired");
			t.add(row.getStringNullable("url")==null?"":"Present");
			t.add(UnixTime.fromUnixTime(row.getIntNullable("urlfirst"),st.getAvatar().getTimeZone()));
			t.add(UnixTime.fromUnixTime(row.getIntNullable("urllast"),st.getAvatar().getTimeZone()));
			t.add(row.getStringNullable("authnode"));
			final Integer zoneId=row.getIntNullable("zoneid");
			t.add(zoneId==null?"":Zone.get(zoneId).getName()+"#"+zoneId);
			final Integer regionId=row.getIntNullable("regionid");
			t.add(regionId==null?"":Region.get(regionId,true).getName()+"[#"+regionId+"]");
			t.add(row.getInt("protocol"));
		}
		return t;
	}
	
	// ----- Internal Statics -----
	
	/**
	 * Purges a character level KV from the entire instance, for all users.
	 *
	 * @param instance Instance to eliminate the KV from
	 * @param key      K to eliminate
	 */
	static void wipeKV(@Nonnull final Instance instance,final String key) {
		final String kvTable="characterkvstore";
		final String mainTable="characters";
		final String idColumn="characterid";
		db().d("delete from "+kvTable+" using "+kvTable+","+mainTable+" where "+kvTable+".k like ? and "+kvTable+"."+
		       idColumn+"="+mainTable+"."+idColumn+" and "+mainTable+".instanceid=?",key,instance.getId());
		kvCache.purgeAll();
	}
	
	/**
	 * Validates a character name against the filtered naming list (forbidden words)
	 *
	 * @param st   State
	 * @param name Name to check
	 * @throws UserInputValidationFilterException if the name uses a prohobited name
	 */
	private static void checkFilteredNamingList(@Nonnull final State st,@Nonnull final String name) {
		// break the users name into components based on certain characters
		final String[] nameParts=name.split("[ ,.\\-]");  // space comma dot dash
		final String[] filterList=st.getKV("Instance.FilteredNamingList").toString().split(",");
		for (String filter: filterList) {
			filter=filter.trim();
			if (!filter.isEmpty()) {
				// compare filter to all name parts
				for (String namePart: nameParts) {
					namePart=namePart.trim();
					if (filter.equalsIgnoreCase(namePart)) {
						throw new UserInputValidationFilterException("Character name contains prohibited word '"+filter+
						                                             "', please reconsider your name.  Please do not simply "+
						                                             "work around this filter as sim staff will not be as easily fooled.",
						                                             true);
					}
				}
			}
		}
	}
	
	/**
	 * Checks if the name contains permitted symbols
	 *
	 * @param st   State
	 * @param name Name to check
	 * @throws UserInputValidationFilterException if the name uses a prohobited symbol
	 */
	private static void checkAllowedNamingSymbols(@Nonnull final State st,@Nonnull String name) {
		// in this approach we eliminate characters we allow.  If the result is an empty string, they win.  Else "uhoh"
		name=name.replaceAll("[A-Za-z ]",""); // alphabetic, space and dash
		final String allowList=st.getKV("Instance.AllowedNamingSymbols").toString();
		for (int i=0;i<allowList.length();i++) {
			final String allow=String.valueOf(allowList.charAt(i));
			name=name.replaceAll(Pattern.quote(allow),"");
		}
		// unique the characters in the string.  There's a better way of doing this surely.
		if (!name.trim().isEmpty()) {
			final StringBuilder blockedChars=new StringBuilder();
			// bad de-duping code
			final Set<String> characters=new HashSet<>(); // just dont like the java type 'character' in this project
			// stick all the symbols in a set :P
			for (int i=0;i<name.length();i++) {
				characters.add(String.valueOf(name.charAt(i)));
			}
			// and reconstitute it
			for (final String character: characters) {
				blockedChars.append(character);
			}
			throw new UserInputValidationFilterException(
					"Disallowed characters present in character name, avoid using the following: "+blockedChars+
					".  Please ensure you are "+"entering JUST A NAME at this point, not descriptive details.",
					true);
		}
	}
	/**
	 * Log out an avatar.
	 *
	 * @param user      Avatar to logout
	 * @param otherThan Character to not log out, or null to log out all by the user
	 */
	public static void logoutByAvatar(@Nonnull final User user,@Nullable final Char otherThan) {
		for (final ResultsRow row: db().dq("select characterid from characters where playedby=?",user.getId())) {
			final Char character=Char.get(row.getInt());
			if (!character.equals(otherThan)) {
				Visit.closeVisits(character,character.getRegion());
				character.disconnect();
			}
		}
	}
	/**
	 * Get the owning avatar for this character.
	 *
	 * @return Avatar owner of this character
	 */
	@Nonnull
	public User getOwner() {
		return ownerCache.get(this,()->User.get(getInt("owner")));
	}/**
	 * Sets the characters callback URL - call me often!.
	 * Only updates the database if the URL has changed.
	 * Also updates the "Last accessed" time if its more than 60 seconds out of date.
	 * Sends a shutdown hudMessage to the old URL if this replaces it.
	 *
	 * @param url URL to set to
	 */
	public void setURL(@Nonnull final String url) {
		final String oldURL=getURL();
		final int now=getUnixTime();
		
		// update last used timer if we're the same URL and its more than 60 seconds since the last timer and we're done
		if (url.equals(oldURL)) {
			refreshURL(url);
			return;
		}
		
		// if there was a URL, send it a shutdown
		if (oldURL!=null&&!(oldURL.isEmpty())) {
			final JSONObject shutdown=new JSONObject().put("incommand","shutdown")
			                                          .put("shutdown",
			                                               "Connection replaced by new character connection");
			final Transmission t=new Transmission(this,shutdown,oldURL);
			t.start();
		}
		// set the URL
		if (url.startsWith("https")) {
			MailTools.logTrace("HTTPS URL violation",url);
		}
		d("update characters set url=?, lastactive=?, urllast=?, urlfirst=?, authnode=? where characterid=?",
		  url,
		  now,
		  now,
		  now,
		  Interface.getNode(),
		  getId());
		
	}
	
	// ---------- INSTANCE ----------
	
public void wipeConveyances(@Nonnull final State st) {
		db().d("delete from characterkvstore where characterid=? and "+"k like 'gphudclient.conveyance-%' and "+
		       "k not like 'gphudclient.conveyance-leveltext'",getId());
		st.purgeCache(this);
		kvCache.purge(this);
	}

		@Deprecated
	public void setActive() {
		db().d("update characters set lastactive=? where characterid=?",getUnixTime()+1,getId());
	}
public void login(final User user,final Region region,final String url) {
		disconnectURL(url);
		logoutByAvatar(user,this);
		d("update characters set playedby=?,lastactive=?,url=?,urlfirst=?,urllast=?,authnode=?,zoneid=?,regionid=? where characterid=?",
		  user.getId(),
		  // played by
		  getUnixTime(),
		  // last active
		  url,
		  // url
		  getUnixTime(),
		  //urlfirst
		  getUnixTime(),
		  // urllast
		  Interface.getNode(),
		  //node
		  null,
		  //zone
		  region.getId(),
		  //region id
		  getId()); // where char id
		zoneCache.purge(this);
	}/**
	 * Gets the characters personal URL
	 *
	 * @return The URL, or null
	 */
	@Nullable
	public String getURL() {
		return getStringNullable("url");
	}
	
	/**
	 * Disconnects a character.  Does not send a terminate to the URL
	 */
	public void disconnect() {
		d("update characters set playedby=?,lastactive=?,url=?,urlfirst=?,urllast=?,authnode=?,zoneid=?,regionid=? where characterid=?",
		  null,
		  //playedby
		  getUnixTime()-1,
		  //lastactive
		  null,
		  //url
		  null,
		  //urlfirst
		  null,
		  //urllast
		  null,
		  //authnode
		  null,
		  //zone
		  null,
		  //region
		  getId()); //character id
		zoneCache.purge(this);
	}
	
	public void wipeConveyance(final State st,final String conveyance) {
		db().d("delete from characterkvstore where characterid=? and k like ?",
		       getId(),
		       "gphudclient.conveyance-"+conveyance);
		st.purgeCache(this);
		kvCache.purge(this);
	}
	/**
	 * Call a characters HUD to get a radar list of nearby Characters.
	 *
	 * @param st State
	 * @return List of nearby Chars
	 */
	@Nonnull
	public List<Char> getNearbyCharacters(@Nonnull final State st) {
		final Char character=st.getCharacter();
		final List<Char> chars=new ArrayList<>();
		final String uri=character.getURL();
		if (uri==null||uri.isEmpty()) {
			throw new UserInputStateException("Your character does not have a valid in-world presence");
		}
		final JSONObject radarRequest=new JSONObject().put("incommand","radar");
		final Transmission t=new Transmission(this,radarRequest);
		//noinspection CallToThreadRun
		t.run();
		final JSONObject j=t.getResponse();
		if (j==null) {
			throw new SystemRemoteFailureException("Failed to get a useful response from the remote HUD");
		}
		final String avatars=j.optString("avatars","");
		if (avatars==null||avatars.isEmpty()) {
			throw new UserInputEmptyException("Sorry, you are not near any other avatars");
		}
		for (final String key: avatars.split(",")) {
			final User a=User.findUserKeyNullable(key);
			if (a!=null) {
				Char c=null;
				try {
					c=Char.getActive(a,st.getInstance());
				} catch (@Nonnull final UserException ignored) {
				}
				if (c!=null) {
					chars.add(c);
				}
			}
		}
		return chars;
	}/**
	 * Update the last-used timestamp on a URL.
	 * Provided it is more than REFRESH_INTERVAL seconds ago (i.e. dont spam the DB with write-updates)
	 * Ignores the request if the URL doesn't exist.
	 *
	 * @param url the URL to refresh the last used timer for.
	 */
	public static void refreshURL(@Nonnull final String url) {
		final String t="characters";
		final int refreshIfOlderThank=getUnixTime()-REFRESH_INTERVAL;
		final int toUpdate=
				db().dqiNotNull("select count(*) from "+t+" where url=? and urllast<?",url,refreshIfOlderThank);
		if (toUpdate==0) {
			return;
		}
		if (toUpdate>1) {
			GPHUD.getLogger().warning("Unexpected anomoly, "+toUpdate+" rows to update on "+t+" url "+url);
		}
		//Log.log(Log.DEBUG,"SYSTEM","DB_Character","Refreshing CHARACTER url "+url);
		if (url.startsWith("https")) {
			MailTools.logTrace("HTTPS URL violation",url);
		}
		db().d("update "+t+" set lastactive=?,urllast=?,authnode=? where url=?",
		       getUnixTime(),
		       getUnixTime(),
		       Interface.getNode(),
		       url);
	}
	
	/**
	 * Gets the characters logged in by the given avatar.
	 *
	 * @param avatar Avatar to look up the logged-in character for
	 * @return Char they are using
	 */
	@Nonnull
	public static Char getActive(@Nonnull final User avatar,@Nonnull final Instance instance) {
		try {
			final int i=db().dqiNotNull("select characterid from characters where playedby=? and instanceid=?",
			                            avatar.getId(),
			                            instance.getId());
			return get(i);
		} catch (@Nonnull final NoDataException e) {
			throw new UserInputStateException(
					"Avatar "+avatar.getName()+" is not wearing the HUD or is not logged in as a character presently.",
					e,
					true);
		}
	}	/**
	 * Get the current region for this character
	 *
	 * @return Region - nulls the retired region
	 */
	@Nullable
	public Region getRegion() {
		final Integer region=getIntNullable("regionid");
		if (region==null) {
			return null;
		}
		final Region r=Region.get(region,true);
		if (r.isRetired()) {
			return null;
		}
		return r;
	}
	
	/**
	 * Gets the level for this character.
	 *
	 * @param st State
	 * @return Level number
	 */
	public int getLevel(@Nonnull final State st) {
		if (st.hasModule("Experience")) {
			int level=Experience.toLevel(st,Experience.getExperience(st,this));
			final int maxLevel=st.getKV("Experience.MaxLevel").intValue();
			if (maxLevel==0) {
				return level;
			}
			if (level>maxLevel) {
				level=maxLevel;
			}
			return level;
		}
		return 0;
	}
	
	/**
	 * Transmits a JSONResponse to a characters HUD
	 *
	 * @param json JSON Response to push
	 */
	public void push(@Nonnull final JSONResponse json) {
		push(json.asJSON(new State(this)));
	}
	
	private static final Cache<Char,User> ownerCache=Cache.getCache("GPHUD/charOwner",CacheConfig.PERMANENT_CONFIG);
	
	/**
	 * Set the owner of this character.
	 *
	 * @param newOwner The new owner
	 */
	public void setOwner(@Nonnull final User newOwner) {
		set("owner",newOwner.getId());
		ownerCache.set(this,newOwner);
		// purge any primary characters referring to this
		// deprecated PrimaryCharacter.purge(this);
	}
	
	@Nonnull
	@Override
	public String getTableName() {
		return "characters";
	}
	
	@Nonnull
	@Override
	public String getIdColumn() {
		return "characterid";
	}
	
	/**
	 * Transmits a JSON object to the characters hud.
	 *
	 * @param json JSON Object to push
	 */
	public void push(@Nonnull final JSONObject json) {
		final String url=getURL();
		if (url==null) {
			return;
		}
		final Transmission t=new Transmission(this,json);
		t.start();
	}
	
	/**
	 * Get the zone this character is in.
	 *
	 * @return Zone
	 */
	@Nullable
	public Zone getZone() {
		return zoneCache.get(this,()->{
			try {
				final Integer id=dqi("select zoneid from characters where characterid=?",getId());
				if (id==null) {
					return null;
				}
				return Zone.get(id);
			} catch (@Nonnull final NoDataException e) {
				return null;
			}
		});
	}
	
	/**
	 * Set the zone this character is in
	 *
	 * @param zone Zone
	 */
	public void setZone(@Nullable final Zone zone) {
		Integer id=null;
		if (zone!=null) {
			id=zone.getId();
		}
		if (id==null) {
			d("update characters set zoneid=null where characterid=?",getId());
			zoneCache.set(this,null);
			return;
		}
		d("update characters set zoneid=? where characterid=?",id,getId());
		zoneCache.set(this,zone);
	}
	
	/**
	 * Get the last played time for this character.
	 *
	 * @return Unix time the character was last played.
	 */
	@Nullable
	public Integer getLastPlayed() {
		return dqi("select lastactive from characters where characterid=?",getId());
	}
	
	/**
	 * Push a HUD Wearer (ownersay) hudMessage to the hud
	 *
	 * @param message Text hudMessage to send.
	 */
	public void hudMessage(final String message) {
		push("message",message);
	}
	
	/**
	 * Transmits a JSON K:V pair to the characters hud.
	 *
	 * @param key   Key
	 * @param value Value
	 */
	public void push(@Nonnull final String key,@Nonnull final String value) {
		final JSONObject j=new JSONObject();
		j.put(key,value);
		push(j);
	}
	
	/**
	 * Set up all conveyances assuming the HUD has no state.
	 *
	 * @param st      State
	 * @param payload Message to append the conveyances to
	 */
	public void initialConveyances(@Nonnull final State st,@Nonnull final JSONObject payload) {
		validate(st);
		final Map<KV,String> oldConveyances=loadConveyances(st);
		for (final Map.Entry<KV,String> entry: oldConveyances.entrySet()) {
			final KV kv=entry.getKey();
			if (kv!=null) {
				final String oldValue=entry.getValue();
				final String newValue=st.getKV(kv.fullName()).value();
				final String conveyAs=kv.conveyAs();
				if (!conveyAs.isEmpty()) {
					payload.put(conveyAs,newValue); // always put in init
					if (!oldValue.equals(newValue)) {
						setKV(st,"gphudclient.conveyance-"+kv.conveyAs(),newValue); // skip cache flush
					}
				}
			}
		}
	}
	
	public void validate(@Nonnull final State st) {
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Char / State Instance mismatch");
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
		return "characters";
	}
	
	// ----- Internal Instance -----
	
	@Nonnull
	@Override
	public String getKVTable() {
		return "characterkvstore";
	}
	
	@Nonnull
	@Override
	public String getKVIdField() {
		return "characterid";
	}
	
	/**
	 * Copy the existing conveyance values from their cached KVs into a map.
	 *
	 * @param st State
	 * @return Map of KV=Values for all conveyanced data.
	 */
	@Nonnull
	private Map<KV,String> loadConveyances(@Nonnull final State st) {
		// load the previously sent conveyances from the DB.  Note that the 'state' caches these queries so its not quite as garbage as it sounds.
		validate(st);
		final Map<KV,String> conveyances=new TreeMap<>();
		for (final KV kv: getConveyances(st)) {
			try {
				conveyances.put(kv,st.getKV("gphudclient.conveyance-"+kv.conveyAs()).value());
			} catch (@Nonnull final SystemException e) {
				st.logger()
				  .log(SEVERE,
				       "Exceptioned loading conveyance "+kv.conveyAs()+" in instance "+st.getInstanceString()+" - "+
				       e.getLocalizedMessage());
			}
		}
		return conveyances;
	}
	
	/**
	 * Get the instance for this character.
	 *
	 * @return The Instance
	 */
	@Nonnull
	public Instance getInstance() {
		return instanceCache.get(this,()->Instance.get(getInt("instanceid")));
	}
	
	@Nonnull
	public User getPlayedBy() {
		final User response=getPlayedByNullable();
		if (response!=null) {
			return response;
		}
		throw new UserInputStateException(
				"Character "+getName()+" is not currently registered as being played by any Agent.");
	}
	
	/**
	 * Select the played by avatar for this character.
	 *
	 * @return Avatar
	 */
	@Nullable
	public User getPlayedByNullable() {
		final Integer avatarId=dqi("select playedby from characters where characterid=?",getId());
		if (avatarId==null) {
			return null;
		}
		return User.get(avatarId);
	}
	
	/**
	 * Update the "is being played by" field on the character sheet
	 *
	 * @param avatar Avatar who is playing this character.
	 */
	public void setPlayedBy(@Nullable final User avatar) {
		if (avatar==null) {
			set("playedby",(Integer)null);
		} else {
			set("playedby",avatar.getId());
		}
	}
	
	/**
	 * Mark this character as retired
	 */
	public void retire() {
		if (retired()) {
			return;
		}
		final String now=new SimpleDateFormat("yyyyMMdd").format(new Date());
		rename(getName()+" (Retired "+now+")");
		set("retired",true);
	}
	
	/**
	 * Is this character retired
	 *
	 * @return true if retired
	 */
	public boolean retired() {
		return getBool("retired");
	}
	
	/**
	 * Rename a character
	 *
	 * @param newName The characters new name
	 */
	public void rename(final String newName) {
		final int count=dqinn("select count(*) from characters where name like ? and instanceid=?",
		                      newName,
		                      getInstance().getId());
		if (count!=0) {
			throw new UserInputDuplicateValueException(
					"Unable to rename character '"+getName()+"' to '"+newName+"', that name is already taken.",true);
		}
		set("name",newName);
		clearNameCache();
	}
	
	/**
	 * Called when a ping to the URL completes, update the timer
	 */
	public void pinged() {
		d("update characters set urllast=? where characterid=?",getUnixTime(),getId());
	}
	
	/**
	 * Checks they have an active URL
	 */
	public boolean isOnline() {
		final String s=getURL();
		return s!=null&&!s.isEmpty();
	}
	
	/**
	 * Appends conveyances and pushes if any have changed
	 */
	public void considerPushingConveyances() {
		final JSONObject json=new JSONObject();
		appendConveyance(new State(this),json);
		//System.out.println("Consider pushing: "+json.toString()+" = "+json.keySet().size());
		if (!json.keySet().isEmpty()) {
			new Transmission(this,json).start();
		}
	}
	
	/**
	 * Append any conveyances that have changed to the payload
	 *
	 * @param st      State
	 * @param payload Message to append changed conveyances to.
	 */
	public void appendConveyance(@Nonnull State st,@Nonnull final JSONObject payload) {
		// SANITY NOTE TO SELF - there is also initialConveyance which does almost exactly the same, which is bad
		validate(st);
		if (st.getCharacter()!=this) {
			st=new State(this);
		}
		final Map<KV,String> oldConveyances=loadConveyances(st);
		for (final Map.Entry<KV,String> entry: oldConveyances.entrySet()) {
			final KV kv=entry.getKey();
			if (kv!=null) {
				final String conveyAs=kv.conveyAs();
				if (!conveyAs.isEmpty()) {
					final String oldValue=entry.getValue();
					final String newValue=st.getKV(kv.fullName()).value();
					if (!oldValue.equals(newValue)) {
						payload.put(conveyAs,newValue);
						setKV(st,"gphudclient.conveyance-"+kv.conveyAs(),newValue); // skip cache update/flush
					}
				}
			}
		}
	}	/**
	 * Set the current region for this character
	 *
	 * @param r Region
	 */
	public void setRegion(@Nonnull final Region r) {
		//System.out.println("Setting region to "+r+" for "+getName()+" where it is currently "+getRegion());
		if (getRegion()!=r) {
			set("regionid",r.getId());
		}
	}
	
	public int getProtocol() {
		return protocolCache.get(this,()->getInt("protocol"));
	}
	
	public void setProtocol(final int protocol) {
		set("protocol",protocol);
		protocolCache.set(this,protocol);
	}
	
	/**
	 * Write the last active timestamp for this character - note this isn't used by GPHUD main control flow, just used for testing
	 */
	public void setLastActive(final Integer value) {
		set("lastactive",value);
	}
	

	

	

	

	

	

	
	/**
	 * Used to load a list of conveyances
	 *
	 * @param st State
	 * @return Set of conveyanced KVs
	 */
	@Nonnull
	private Set<KV> getConveyances(@Nonnull final State st) {
		// load the previously sent conveyances from the DB.  Note that the 'state' caches these queries so its not quite as garbage as it sounds.
		validate(st);
		final Set<KV> conveyances=new TreeSet<>();
		for (final KV kv: Modules.getKVSet(st)) {
			if (kv!=null) {
				final String conveyAs=kv.conveyAs();
				if (!conveyAs.isEmpty()) {
					conveyances.add(kv);
				}
			}
		}
		return conveyances;
	}
	

	

	

	

	

}
