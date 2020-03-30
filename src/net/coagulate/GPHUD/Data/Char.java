package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemRemoteFailureException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputEmptyException;
import net.coagulate.Core.Exceptions.User.UserInputInvalidChoiceException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
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
import static net.coagulate.Core.Tools.UnixTime.duration;
import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * Reference to a character within an instance
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Char extends TableRow {

	protected Char(final int id) { super(id); }

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 *
	 * @return A Char representation
	 */
	@Nonnull
	public static Char get(final int id) {
		return (Char) factoryPut("Character",id,new Char(id));
	}

	/**
	 * Update the last-used timestamp on a URL.
	 * Provided it is more than REFRESH_INTERVAL seconds ago (i.e. dont spam the DB with write-updates)
	 * Ignores the request if the URL doesn't exist.
	 *
	 * @param url the URL to refresh the last used timer for.
	 */
	public static void refreshURL(@Nonnull final String url) {
		final String t="characters";
		final int refreshifolderthan=getUnixTime()-REFRESH_INTERVAL;
		final int toupdate=GPHUD.getDB().dqinn("select count(*) from "+t+" where url=? and urllast<?",url,refreshifolderthan);
		if (toupdate==0) { return; }
		if (toupdate>1) {
			GPHUD.getLogger().warning("Unexpected anomoly, "+toupdate+" rows to update on "+t+" url "+url);
		}
		//Log.log(Log.DEBUG,"SYSTEM","DB_Character","Refreshing CHARACTER url "+url);
		if (url.startsWith("https")) { MailTools.logTrace("HTTPS URL violation",url); }
		GPHUD.getDB().d("update "+t+" set lastactive=?,urllast=?,authnode=? where url=?",getUnixTime(),getUnixTime(),Interface.getNode(),url);
	}

	/**
	 * Gets the characters logged in by the given avatar.
	 *
	 * @param avatar Avatar to look up the logged-in character for
	 *
	 * @return Char they are using
	 */
	@Nonnull
	public static Char getActive(@Nonnull final User avatar,
	                             @Nonnull final Instance instance) {
		try {
			final int i=GPHUD.getDB().dqinn("select characterid from characters where playedby=? and instanceid=?",avatar.getId(),instance.getId());
			return get(i);
		}
		catch (@Nonnull final NoDataException e) {
			throw new UserInputStateException("Avatar "+avatar.getName()+" is not wearing the HUD or is not logged in as a character presently.",e,true);
		}
	}

	/**
	 * Return a set of all characters inside a given zone.
	 *
	 * @param zone The zone to enumerate
	 *
	 * @return A set of Char(acters) that are registered inside the zone.
	 */
	@Nonnull
	public static Set<Char> getInZone(@Nonnull final Zone zone) {
		final Set<Char> chars=new TreeSet<>();
		for (final ResultsRow r: GPHUD.getDB().dq("select characterid from characters where zoneid=? and url is not null",zone.getId())) {
			chars.add(Char.get(r.getInt()));
		}
		return chars;
	}

	/**
	 * Purges a character level KV from the entire instance, for all users.
	 *
	 * @param instance Instance to eliminate the KV from
	 * @param key      K to eliminate
	 */
	static void wipeKV(@Nonnull final Instance instance,
	                   final String key) {
		final String kvtable="characterkvstore";
		final String maintable="characters";
		final String idcolumn="characterid";
		GPHUD.getDB()
		     .d("delete from "+kvtable+" using "+kvtable+","+maintable+" where "+kvtable+".k like ? and "+kvtable+"."+idcolumn+"="+maintable+"."+idcolumn+" and "+maintable+
				        ".instanceid=?",
		        key,
		        instance.getId()
		       );
	}

	/**
	 * Find character by name
	 *
	 * @param st   State
	 * @param name Character name
	 *
	 * @return Character
	 */
	@Nullable
	public static Char resolve(@Nonnull final State st,
	                           final String name) {
		final int id=new Char(-1).resolveToID(st,name,true);
		if (id==0) { return null; }
		return get(id);
	}

	/**
	 * Gets a list of all active characters
	 * Specifically those with inbound URLs
	 *
	 * @param i Instance
	 *
	 * @return Set of Characters that have inbound links
	 */
	@Nonnull
	public static Set<Char> getActive(final Instance i) {
		final Set<Char> chars=new TreeSet<>();
		for (final ResultsRow r: GPHUD.getDB().dq("select characterid from characters where url is not null")) {
			chars.add(get(r.getInt()));
		}
		return chars;
	}

	/**
	 * Get list of a users non-retired characters at a given instance.
	 *
	 * @param instance The instance
	 * @param avatar   The user to get a list of characters for
	 *
	 * @return List of unretired Char (characters)
	 */
	@Nonnull
	public static Set<Char> getCharacters(@Nonnull final Instance instance,
	                                      @Nonnull final User avatar) {
		final Results rows=GPHUD.getDB().dq("select characterid from characters where owner=? and retired=0 and instanceid=?",avatar.getId(),instance.getId());
		final Set<Char> results=new TreeSet<>();
		for (final ResultsRow r: rows) {
			results.add(Char.get(r.getInt()));
		}
		return results;
	}

	public static void create(@Nonnull final State st,
	                          @Nonnull final String name,
	                          final boolean filter) {
		if (filter) {
			// check some KVs about the name..
			//check Instance.AllowedNamingSymbols
			checkAllowedNamingSymbols(st,name);
			//check Instance.FilteredNamingList
			checkFilteredNamingList(st,name);
		}
		GPHUD.getDB()
		     .d("insert into characters(name,instanceid,owner,lastactive,retired) values(?,?,?,?,?)",name,st.getInstance().getId(),st.getAvatar().getId(),getUnixTime(),0);
	}

	private static void checkFilteredNamingList(@Nonnull final State st,
	                                            @Nonnull final String name) {
		// break the users name into components based on certain characters
		final String[] nameparts=name.split("[ ,\\.\\-]");  // space comma dot dash
		final String[] filterlist=st.getKV("Instance.FilteredNamingList").toString().split(",");
		for (String filter:filterlist) {
			filter=filter.trim();
			if (!filter.isEmpty()) {
				// compare filter to all name parts
				for (String namepart:nameparts) {
					namepart=namepart.trim();
					if (filter.equalsIgnoreCase(namepart)) {
						throw new UserInputInvalidChoiceException("Character name contains prohibited word '"+filter+"', please reconsider your name.  Please do not simply work around this filter as sim staff will not be as easily fooled.");
					}
				}
			}
		}
	}

	private static void checkAllowedNamingSymbols(@Nonnull final State st,
	                                              @Nonnull String name) {
		// in this approach we eliminate characters we allow.  If the result is an empty string, they win.  Else "uhoh"
		name=name.replaceAll("[A-Za-z ]",""); // alphabetic, space and dash
		final String allowlist=st.getKV("Instance.AllowedNamingSymbols").toString();
		for (int i=0;i<allowlist.length();i++) {
			final String allow=allowlist.charAt(i)+"";
			name=name.replaceAll(Pattern.quote(allow),"");
		}
		// unique the characters in the string.  There's a better way of doing this surely.
		if (!name.trim().isEmpty()) {
			StringBuilder blockedchars=new StringBuilder();
			// bad de-duping code
			final Set<String> characters=new HashSet<>(); // just dont like the java type 'character' in this project
			// stick all the symbols in a set :P
			for (int i=0;i<name.length();i++) { characters.add(name.charAt(i)+""); }
			// and reconstitute it
			for (final String character:characters) { blockedchars.append(character); }
			throw new UserInputInvalidChoiceException("Disallowed characters present in character name, avoid using the following: "+blockedchars+".  Please ensure you are entering JUST A NAME at this point, not descriptive details.");
		}
	}

	/**
	 * Get all characters for the avatar, at any instance.
	 *
	 * @return Set of Char that are owned by this avatar and not retired
	 */
	@Nonnull
	public static Set<Char> getCharacters(@Nonnull final User a) {
		final Results rows=GPHUD.getDB().dq("select characterid from characters where owner=? and retired=0",a.getId());
		final Set<Char> results=new TreeSet<>();
		for (final ResultsRow r: rows) {
			results.add(Char.get(r.getInt()));
		}
		return results;
	}

	/**
	 * Get most recent character an avatar used, from any instance
	 *
	 * @param avatar User to look up
	 *
	 * @return Most recent character used, possibly null.
	 */
	@Nullable
	public static Char getMostRecent(@Nonnull final User avatar) {
		final Results results=GPHUD.getDB().dq("select characterid from characters where owner=? and retired=0 order by lastactive desc limit 0,1",avatar.getId());
		if (results.empty()) { return null; }
		try {
			return Char.get(results.iterator().next().getInt("characterid"));
		}
		catch (@Nonnull final Exception e) { // weird
			GPHUD.getLogger().log(SEVERE,"Exception while instansiating most recently used character?",e);
			return null;
		}
	}

	/**
	 * Get most recent character used at an instance
	 *
	 * @param avatar           User to look up most recent character for
	 * @param optionalinstance Specific instance to search in, or null for find in all instances
	 *
	 * @return Most recently used character matching parameters, or null
	 */
	@Nullable
	public static Char getMostRecent(@Nonnull final User avatar,
	                                 @Nullable final Instance optionalinstance) {
		if (optionalinstance==null) { return getMostRecent(avatar); }
		final Results results=GPHUD.getDB()
		                           .dq("select characterid from characters where owner=? and retired=0 and instanceid=? order by lastactive desc limit 0,1",
		                               avatar.getId(),
		                               optionalinstance.getId()
		                              );
		if (results.empty()) { return null; }
		try {
			return Char.get(results.iterator().next().getInt("characterid"));
		}
		catch (@Nonnull final Exception e) { // weird
			GPHUD.getLogger().log(SEVERE,"Exception while instansiating most recently used character?",e);
			return null;
		}
	}

	/**
	 * Return a HTML list of userid/username of NPCs at this instance
	 *
	 * @param st       Inters instance
	 * @param listname name attribute of the HTML list
	 *
	 * @return A DropDownList
	 */
	@Nonnull
	public static DropDownList getNPCList(@Nonnull final State st,
	                                      final String listname) {
		final DropDownList list=new DropDownList(listname);
		for (final ResultsRow row: GPHUD.getDB()
		                                .dq("select characterid,name from characters where instanceid=? and owner=?",st.getInstance().getId(),User.getSystem().getId())) {
			list.add(row.getIntNullable("characterid")+"",row.getStringNullable("name"));
		}
		return list;
	}

	public static Results getPingable() {
		return GPHUD.getDB()
		     .dq("select characterid,name,url,urllast from characters where url is not null and authnode like ? and urllast<? order by urllast asc "+
				         "limit 0,30",
		         Interface.getNode(),
		         UnixTime.getUnixTime()-(Maintenance.PINGHUDINTERVAL*60)
		        );
	}

	/**
	 * Gets the characters personal URL
	 *
	 * @return The URL, or null
	 */
	@Nullable
	public String getURL() {
		return getStringNullable("url");
	}

	/**
	 * Sets the characters callback URL - call me often!.
	 * Only updates the database if the URL has changed.
	 * Also updates the "Last accessed" time if its more than 60 seconds out of date.
	 * Sends a shutdown hudMessage to the old URL if this replaces it.
	 *
	 * @param url URL to set to
	 */
	public void setURL(@Nonnull final String url) {
		final String oldurl=getURL();
		final int now=getUnixTime();

		// update last used timer if we're the same URL and its more than 60 seconds since the last timer and we're done
		if (url.equals(oldurl)) {
			refreshURL(url);
			return;
		}

		// if there was a URL, send it a shutdown
		if (oldurl!=null && !("".equals(oldurl))) {
			final JSONObject shutdown=new JSONObject().put("incommand","shutdown").put("shutdown","Connection replaced by new character connection");
			final Transmission t=new Transmission(this,shutdown,oldurl);
			t.start();
		}
		// set the URL
		if (url.startsWith("https")) { MailTools.logTrace("HTTPS URL violation",url); }
		d("update characters set url=?, lastactive=?, urllast=?, urlfirst=?, authnode=? where characterid=?",url,now,now,now,Interface.getNode(),getId());

	}

	/**
	 * Gets the last active time for the URL.
	 *
	 * @return Unix format timestamp for the last use of this URL - accurate to within REFRESH_INTERVAL
	 */
	@Nullable
	public Integer getURLLast() {
		return getIntNullable("urllast");
	}

	/**
	 * Get the instance for this character.
	 *
	 * @return The Instance
	 */
	@Nonnull
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}

	/**
	 * Get the owning avatar for this character.
	 *
	 * @return Avatar owner of this character
	 */
	@Nonnull
	public User getOwner() {
		return User.get(getInt("owner"));
	}

	public void setOwner(@Nonnull final User newowner) {
		set("owner",newowner.getId());
		// purge any primary characters referring to this
		PrimaryCharacters.purge(this);
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "characters";
	}

	@Nonnull
	@Override
	public String getIdField() {
		return "characterid";
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}

	@Nonnull
	@Override
	public String getLinkTarget() { return "characters"; }

	/**
	 * Sum all the entries in a Pool
	 *
	 * @param p Pool
	 *
	 * @return Sum of the entries
	 */
	public int sumPool(@Nonnull final Pool p) {
		final Integer sum=dqi("select sum(adjustment) from characterpools where characterid=? and poolname like ?",getId(),p.fullName());
		if (sum==null) { return 0; }
		return sum;
	}

	/**
	 * Add an adjustment to a pool from a character.
	 *
	 * @param st          State of the grantor (requires character)
	 * @param p           Pool
	 * @param adjustment  Ammount to grant
	 * @param description Audit logged description
	 */
	public void addPool(@Nonnull final State st,
	                    @Nonnull final Pool p,
	                    final int adjustment,
	                    final String description) {
		d("insert into characterpools(characterid,poolname,adjustment,adjustedbycharacter,adjustedbyavatar,description,timedate) values(?,?,?,?,?,?,?)",
		  getId(),
		  p.fullName(),
		  adjustment,
		  st.getCharacter().getId(),
		  st.getAvatar().getId(),
		  description,
		  getUnixTime()
		 );
	}

	/**
	 * Add an adjustment to a pool, as an administrator (Avatar).
	 *
	 * @param st          State of the grantor (requires avatar)
	 * @param p           Pool
	 * @param adjustment  Ammount to grant
	 * @param description Audit logged description
	 */
	public void addPoolAdmin(@Nonnull final State st,
	                         @Nonnull final Pool p,
	                         final int adjustment,
	                         final String description) {
		d("insert into characterpools(characterid,poolname,adjustment,adjustedbycharacter,adjustedbyavatar,description,timedate) values(?,?,?,?,?,?,?)",
		  getId(),
		  p.fullName(),
		  adjustment,
		  null,
		  st.getAvatar().getId(),
		  description,
		  getUnixTime()
		 );
	}

	/**
	 * Add an adjustment to a pool on behalf of SYSTEM.
	 *
	 * @param st          State (not used)
	 * @param p           Pool
	 * @param adjustment  Ammount to adjust
	 * @param description Logged reason for the change
	 */
	public void addPoolSystem(final State st,
	                          @Nonnull final Pool p,
	                          final int adjustment,
	                          final String description) {
		d("insert into characterpools(characterid,poolname,adjustment,adjustedbycharacter,adjustedbyavatar,description,timedate) values(?,?,?,?,?,?,?)",
		  getId(),
		  p.fullName(),
		  adjustment,
		  null,
		  User.getSystem().getId(),
		  description,
		  getUnixTime()
		 );
	}

	/**
	 * Sum visit time on sim.
	 *
	 * @param since Ignore visits that end before this time (Unix Time)
	 *
	 * @return Total number of seconds the character has visited the sim for since the specified time
	 */
	public int sumVisits(final int since) {
		final int now=getUnixTime();
		final Results visits=dq("select starttime,endtime from visits where characterid=? and (endtime is null or endtime>?)",getId(),since);
		int seconds=0;
		for (final ResultsRow r: visits) {
			Integer end=r.getIntNullable("endtime");
			final Integer start=r.getIntNullable("starttime");
			if (end==null) { end=now; }
			if (start!=null) { seconds=seconds+(end-start); }
		}
		return seconds;
	}

	/**
	 * Sum a pool since a given time
	 *
	 * @param p     Pool
	 * @param since Unix Time to count points since
	 *
	 * @return Number of points in the given period.
	 */
	public int sumPoolSince(@Nonnull final Pool p,
	                        final int since) {
		final Integer sum=dqi("select sum(adjustment) from characterpools where characterid=? and poolname like ? and timedate>=?",getId(),p.fullName(),since);
		if (sum==null) { return 0; }
		return sum;
	}

	/**
	 * Sum a pool since a given number of days
	 *
	 * @param p    Pool
	 * @param days Number of days ago to start counting from.
	 *
	 * @return Number of points in the pool in the selected time range.
	 */
	public int sumPoolDays(@Nonnull final Pool p,
	                       final float days) {
		final int seconds=(int) (days*60.0*60.0*24.0);
		return sumPoolSince(p,getUnixTime()-seconds);
	}

	/**
	 * Call a characters HUD to get a radar list of nearby Characters.
	 *
	 * @param st State
	 *
	 * @return List of nearby Chars
	 */
	@Nonnull
	public List<Char> getNearbyCharacters(@Nonnull final State st) {
		final Char character=st.getCharacter();
		final boolean debug=false;
		final List<Char> chars=new ArrayList<>();
		final String uri=character.getURL();
		if (uri==null || uri.isEmpty()) {
			throw new UserInputStateException("Your character does not have a valid in-world presence");
		}
		final JSONObject radarrequest=new JSONObject().put("incommand","radar");
		final Transmission t=new Transmission(this,radarrequest);
		//noinspection CallToThreadRun
		t.run();
		final JSONObject j=t.getResponse();
		if (j==null) { throw new SystemRemoteFailureException("Failed to get a useful response from the remote HUD"); }
		final String avatars=j.optString("avatars","");
		if (avatars==null || avatars.isEmpty()) {
			throw new UserInputEmptyException("Sorry, you are not near any other avatars");
		}
		for (final String key: avatars.split(",")) {
			final User a=User.findOptional(key);
			if (a!=null) {
				Char c=null;
				try { c=Char.getActive(a,st.getInstance()); }
				catch (@Nonnull final UserException e) {
				}
				if (c!=null) { chars.add(c); }
			}
		}
		return chars;
	}

	/**
	 * Gets the level for this character.
	 *
	 * @param st State
	 *
	 * @return Level number
	 */
	public int getLevel(@Nonnull final State st) {
		if (st.hasModule("Experience")) { return Experience.toLevel(st,Experience.getExperience(st,this)); }
		return 0;
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
	 * Get all the pools this character has.
	 *
	 * @param st State
	 *
	 * @return List of Pools
	 */
	@Nonnull
	public Set<Pool> getPools(@Nonnull final State st) {
		final Set<Pool> pools=new TreeSet<>();
		final Results results=dq("select distinct poolname from characterpools where characterid=?",getId());
		for (final ResultsRow r: results) {
			final String name=r.getString();
			if (st.hasModule(name)) {
				final Pool p=Modules.getPoolNullable(st,name);
				pools.add(p);
			}
		}
		return pools;
	}

	/**
	 * Get the character group of a given type.
	 *
	 * @param grouptype Group type string
	 *
	 * @return The CharacterGroup or null
	 */
	@Nullable
	public CharacterGroup getGroup(final String grouptype) {
		try {
			final Integer group=dqi(
					"select charactergroups.charactergroupid from charactergroups inner join charactergroupmembers on charactergroups.charactergroupid=charactergroupmembers"+".charactergroupid where characterid=? and charactergroups.type=?",
					getId(),
					grouptype
			                       );
			if (group==null) { return null; }
			return CharacterGroup.get(group);
		}
		catch (@Nonnull final NoDataException e) { return null; }
	}

	/**
	 * Calculate the next free point time string for a pool.
	 *
	 * @param pool  Pool
	 * @param maxxp Maximum ammount of XP earnable in a period
	 * @param days  Period (days)
	 *
	 * @return Explanation of when the next point is available.
	 */
	@Nonnull
	public String poolNextFree(@Nonnull final Pool pool,
	                           final int maxxp,
	                           final float days) {
		if (maxxp==0) { return "NEVER"; }
		final int now=getUnixTime();
		final int nextfree=poolNextFreeAt(pool,maxxp,days);
		if (now >= nextfree) { return "NOW"; }

		final int duration=nextfree-now;
		return "in "+duration(duration,false);
	}

	/**
	 * Calculate the date-time of the next free point for a pool.
	 *
	 * @param pool  Pool
	 * @param maxxp Maximum ammount of XP in a period
	 * @param days  Period in days
	 *
	 * @return Date-time of the point of next free (may be in the past, in which case available NOW).
	 */
	public int poolNextFreeAt(@Nonnull final Pool pool,
	                          final int maxxp,
	                          final float days) {
		final boolean debug=false;
		final int now=getUnixTime();
		final int since=(int) (now-(days*60*60*24));
		final Results res=dq("select adjustment,timedate from characterpools where characterid=? and poolname=? and timedate>?",getId(),pool.fullName(),since);
		int awarded=0;
		final Map<Integer,Integer> when=new TreeMap<>(); // map time stamps to award.
		for (final ResultsRow r: res) {
			final int ammount=r.getInt("adjustment");
			int at=r.getInt("timedate");
			awarded+=ammount;
			while (when.containsKey(at)) { at++; }
			when.put(at,ammount);
		}
		int overshoot=awarded-maxxp;
		if (overshoot<0) { return now; }
		final int datefilled=0;
		for (final Map.Entry<Integer,Integer> entry: when.entrySet()) {
			final int ammount=entry.getValue();
			overshoot-=ammount;
			if (overshoot<0) {
				return (int) (entry.getKey()+(days*60*60*24));
			}
		}
		return now;
	}

	/**
	 * Transmits a JSON K:V pair to the characters hud.
	 *
	 * @param key   Key
	 * @param value Value
	 */
	public void push(@Nonnull final String key,
	                 @Nonnull final String value) {
		final String url=getURL();
		if (url==null) { return; }
		final JSONObject j=new JSONObject();
		j.put(key,value);
		final Transmission t=new Transmission(this,j);
		t.start();
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
	 * Count the number of queued/offline messages available to this user.
	 *
	 * @return Message count
	 */
	public int messages() { return Message.count(this); }

	/**
	 * Push the message count to the client's HUD.
	 */
	public void pushMessageCount() { push("messagecount",Integer.toString(messages())); }

	/**
	 * Log a queued/offline queueMessage
	 *
	 * @param message         Message to send (JSON format)
	 * @param lifespanseconds life span of the queueMessage in seconds
	 */
	public void queueMessage(@Nonnull final JSONObject message,
	                         final int lifespanseconds) {
		Message.add(this,getUnixTime()+lifespanseconds,message);
		pushMessageCount();
	}

	/**
	 * get the next queued message and marks it as active
	 *
	 * @return Message object or null if none
	 */
	@Nullable
	public Message getMessage() {
		return Message.getNextMessage(this);
	}

	/**
	 * gets the currently active message
	 *
	 * @return Message object, or null if no currently active message;
	 */
	@Nullable
	public Message getActiveMessage() { return Message.getActiveMessage(this); }

	/**
	 * Get the zone this character is in.
	 *
	 * @return Zone
	 */
	@Nullable
	public Zone getZone() {
		try {
			final Integer id=dqi("select zoneid from characters where characterid=?",getId());
			if (id==null) { return null; }
			return Zone.get(id);
		}
		catch (@Nonnull final NoDataException e) { return null; }
	}

	/**
	 * Set the zone this character is in
	 *
	 * @param zone Zone
	 */
	public void setZone(@Nullable final Zone zone) {
		Integer id=null;
		if (zone!=null) { id=zone.getId(); }
		if (id==null) {
			d("update characters set zoneid=null where characterid=?",getId());
			return;
		}
		d("update characters set zoneid=? where characterid=?",id,getId());
	}

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
	 * Get all the groups this character is in
	 *
	 * @return Set of Character Groups
	 */
	@Nonnull
	public Set<CharacterGroup> getGroups() {
		final Set<CharacterGroup> ret=new TreeSet<>();
		for (final ResultsRow r: dq("select charactergroupid from charactergroupmembers where characterid=?",getId())) {
			ret.add(CharacterGroup.get(r.getInt()));
		}
		return ret;
	}

	/**
	 * Used to load a list of conveyances
	 *
	 * @param st State
	 *
	 * @return Set of conveyanced KVs
	 */
	@Nonnull
	private Set<KV> getConveyances(@Nonnull final State st) {
		// load the previously sent conveyances from the DB.  Note that the 'state' caches these queries so its not quite as garbage as it sounds.
		validate(st);
		final Set<KV> conveyances=new TreeSet<>();
		for (final KV kv: Modules.getKVSet(st)) {
			if (kv!=null) {
				final String conveyas=kv.conveyas();
				if (!conveyas.isEmpty()) {
					conveyances.add(kv);
				}
			}
		}
		return conveyances;
	}

	/**
	 * Copy the existing conveyance values from their cached KVs into a map.
	 *
	 * @param st State
	 *
	 * @return Map of KV=Values for all conveyanced data.
	 */
	@Nonnull
	private Map<KV,String> loadConveyances(@Nonnull final State st) {
		// load the previously sent conveyances from the DB.  Note that the 'state' caches these queries so its not quite as garbage as it sounds.
		validate(st);
		final Map<KV,String> conveyances=new TreeMap<>();
		for (final KV kv: getConveyances(st)) {
			try {
				conveyances.put(kv,st.getKV("gphudclient.conveyance-"+kv.conveyas()).value());
			}
			catch (@Nonnull final SystemException e) {
				st.logger().log(SEVERE,"Exceptioned loading conveyance "+kv.conveyas()+" in instance "+st.getInstanceString()+" - "+e.getLocalizedMessage());
			}
		}
		return conveyances;
	}

	/**
	 * Set up all conveyances assuming the HUD has no state.
	 *
	 * @param st      State
	 * @param payload Message to append the conveyances to
	 */
	public void initialConveyances(@Nonnull final State st,
	                               @Nonnull final JSONObject payload) {
		final boolean debug=false;
		validate(st);
		final Map<KV,String> oldconveyances=loadConveyances(st);
		for (final Map.Entry<KV,String> entry: oldconveyances.entrySet()) {
			final KV kv=entry.getKey();
			if (kv!=null) {
				final String oldvalue=entry.getValue();
				final String newvalue=st.getKV(kv.fullname()).value();
				final String conveyas=kv.conveyas();
				if (!conveyas.isEmpty()) {
					payload.put(conveyas,newvalue); // always put in init
					if (!oldvalue.equals(newvalue)) {
						setKV(st,"gphudclient.conveyance-"+kv.conveyas(),newvalue); // skip cache flush
					}
				}
			}
		}
	}

	/**
	 * Append any conveyances that have changed to the payload
	 *
	 * @param st      State
	 * @param payload Message to append changed conveyances to.
	 */
	public void appendConveyance(@Nonnull final State st,
	                             @Nonnull final JSONObject payload) {
		// SANITY NOTE TO SELF - there is also initialConveyance which does almost exactly the same, which is bad
		final boolean debug=false;
		validate(st);
		final Map<KV,String> oldconveyances=loadConveyances(st);
		for (final Map.Entry<KV,String> entry: oldconveyances.entrySet()) {
			final KV kv=entry.getKey();
			if (kv!=null) {
				final String oldvalue=entry.getValue();
				final String newvalue=st.getKV(kv.fullname()).value();
				final String conveyas=kv.conveyas();
				if (!conveyas.isEmpty()) {
					if (!oldvalue.equals(newvalue)) {
						payload.put(conveyas,newvalue);
						setKV(st,"gphudclient.conveyance-"+kv.conveyas(),newvalue); // skip cache update/flush
					}
				}
			}
		}
	}

	/**
	 * Get the current region for this character
	 *
	 * @return Region - nulls the retired region
	 */
	@Nullable
	public Region getRegion() {
		final Integer region=getIntNullable("regionid");
		if (region==null) { return null; }
		final Region r=Region.get(region,true);
		if (r.isRetired()) { return null; }
		return r;
	}

	/**
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

	/**
	 * Select the played by avatar for this character.
	 *
	 * @return Avatar
	 */
	@Nullable
	public User getPlayedBy() {
		final Integer avatarid=dqi("select playedby from characters where characterid=?",getId());
		if (avatarid==null) { return null; }
		return User.get(avatarid);
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

	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
		if (st.getInstance()!=getInstance()) { throw new SystemConsistencyException("Char / State Instance mismatch"); }
	}

	protected int getNameCacheTime() { return 5; } // characters /may/ be renamable, just not really sure at this point

	public void retire() {
		if (retired()) { return; }
		final String now=new SimpleDateFormat("yyyyMMdd").format(new Date());
		rename(getName()+" (Retired "+now+")");
		set("retired",true);
	}

	public boolean retired() {
		return getBool("retired");
	}

	public void rename(final String newname) {
		final int count=dqinn("select count(*) from characters where name like ? and instanceid=?",newname,getInstance().getId());
		if (count!=0) {
			throw new UserInputDuplicateValueException("Unable to rename character '"+getName()+"' to '"+newname+"', that name is already taken.");
		}
		set("name",newname);
	}

	public void closeVisits(@Nonnull final State st) {
		if (st.getInstance()!=getInstance()) { throw new IllegalStateException("State character instanceid mismatch"); }
		d("update eventvisits set endtime=UNIX_TIMESTAMP() where characterid=?",getId());
		d("update visits set endtime=UNIX_TIMESTAMP() where characterid=? and regionid=? and endtime is null",getId(),st.getRegion().getId());
	}

	public void closeURL(@Nonnull final State st) {
		if (st.getInstance()!=getInstance()) { throw new IllegalStateException("State character instanceid mismatch"); }
		d("update characters set url=null,urlfirst=null,urllast=null,authnode=null,zoneid=null,regionid=null,lastactive=UNIX_TIMESTAMP(),playedby=null where characterid=?",
		  st.getCharacter().getId()
		 );
	}

	/**
	 * Called when a ping to the URL completes, update the timer
	 */
	public void pinged() {
		d("update characters set urllast=? where characterid=?",UnixTime.getUnixTime(),getId());
	}

	/**
	 * Checks they have an active URL
	 */
	public boolean isOnline() {
		final String s=getURL();
		if (s==null || s.isEmpty()) { return false; }
		return true;
	}

	/**
	 * Appends conveyances and pushes if any have changed
	 */
	public void considerPushingConveyances() {
		final JSONObject json=new JSONObject();
		appendConveyance(new State(this),json);
		//System.out.println("Consider pushing: "+json.toString()+" = "+json.keySet().size());
		if (json.keySet().size()>0) {
			new Transmission(this,json).start();
		}
	}
}
