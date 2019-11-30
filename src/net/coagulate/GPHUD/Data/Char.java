package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
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

import static java.util.logging.Level.SEVERE;
import static net.coagulate.Core.Tools.UnixTime.duration;
import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * Reference to a character within an instance
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Char extends TableRow {

	protected Char(int id) { super(id); }

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A Char representation
	 */
	@Nonnull
	public static Char get(int id) {
		return (Char) factoryPut("Character", id, new Char(id));
	}

	/**
	 * Update the last-used timestamp on a URL.
	 * Provided it is more than REFRESH_INTERVAL seconds ago (i.e. dont spam the DB with write-updates)
	 *
	 * @param url the URL to refresh the last used timer for.
	 */
	public static void refreshURL(@Nonnull String url) {
		String t = "characters";
		int refreshifolderthan = getUnixTime() - REFRESH_INTERVAL;
		int toupdate = GPHUD.getDB().dqi(true, "select count(*) from " + t + " where url=? and urllast<?", url, refreshifolderthan);
		if (toupdate == 0) { return; }
		if (toupdate > 1) {
			GPHUD.getLogger().warning("Unexpected anomoly, " + toupdate + " rows to update on " + t + " url " + url);
		}
		//Log.log(Log.DEBUG,"SYSTEM","DB_Character","Refreshing CHARACTER url "+url);
		if (url.startsWith("https")) { MailTools.logTrace("HTTPS URL violation",url); }
		GPHUD.getDB().d("update " + t + " set lastactive=?,urllast=?,authnode=? where url=?", getUnixTime(), getUnixTime(), Interface.getNode(), url);
	}

	/**
	 * Gets the characters logged in by the given avatar.
	 *
	 * @param avatar Avatar to look up the logged-in character for
	 * @return Char they are using
	 */
	@Nonnull
	public static Char getActive(@Nonnull User avatar, @Nonnull Instance instance) {
		Integer i = GPHUD.getDB().dqi(false, "select characterid from characters where playedby=? and instanceid=?", avatar.getId(), instance.getId());
		if (i == null) {
			throw new UserException("Avatar " + avatar.getName() + " is not wearing the HUD or is not logged in as a character presently.");
		}
		return get(i);
	}

	/**
	 * Return a set of all characters inside a given zone.
	 *
	 * @param zone The zone to enumerate
	 * @return A set of Char(acters) that are registered inside the zone.
	 */
	@Nonnull
	public static Set<Char> getInZone(@Nonnull Zone zone) {
		Set<Char> chars = new TreeSet<>();
		for (ResultsRow r : GPHUD.getDB().dq("select characterid from characters where zoneid=? and url is not null", zone.getId())) {
			chars.add(Char.get(r.getInt()));
		}
		return chars;
	}

	static void wipeKV(@Nonnull Instance instance, String key) {
		String kvtable = "characterkvstore";
		String maintable = "characters";
		String idcolumn = "characterid";
		GPHUD.getDB().d("delete from " + kvtable + " using " + kvtable + "," + maintable + " where " + kvtable + ".k like ? and " + kvtable + "." + idcolumn + "=" + maintable + "." + idcolumn + " and " + maintable + ".instanceid=?", key, instance.getId());
	}

	/**
	 * Find character by name
	 *
	 * @param st   State
	 * @param name Character name
	 * @return Character
	 */
	@Nullable
	public static Char resolve(State st, String name) {
		int id = new Char(-1).resolveToID(st, name, true);
		if (id == 0) { return null; }
		return get(id);
	}

	/**
	 * Gets a list of all active characters
	 * Specifically those with inbound URLs
	 *
	 * @param i Instance
	 * @return Set of Characters that have inbound links
	 */
	@Nonnull
	public static Set<Char> getActive(Instance i) {
		Set<Char> chars = new TreeSet<>();
		for (ResultsRow r : GPHUD.getDB().dq("select characterid from characters where url is not null")) {
			chars.add(get(r.getInt()));
		}
		return chars;
	}

	/**
	 * Get list of users characters at a given instance.
	 *
	 * @param instance The instance
	 * @return List of Char (characters)
	 */
	@Nonnull
	public static Set<Char> getCharacters(@Nonnull Instance instance, @Nonnull User avatar) {
		Results rows = GPHUD.getDB().dq("select characterid from characters where owner=? and retired=0 and instanceid=?", avatar.getId(), instance.getId());
		Set<Char> results = new TreeSet<>();
		for (ResultsRow r : rows) {
			results.add(Char.get(r.getInt()));
		}
		return results;
	}

	public static void create(@Nonnull State st, String name) {
		GPHUD.getDB().d("insert into characters(name,instanceid,owner,lastactive,retired) values(?,?,?,?,?)", name, st.getInstance().getId(), st.getAvatar().getId(), getUnixTime(), 0);
	}

	/**
	 * Get all characters for the avatar, at any instance.
	 *
	 * @return Set of Char that are owned by this avatar and not retired
	 */
	@Nonnull
	public static Set<Char> getCharacters(@Nonnull User a) {
		Results rows = GPHUD.getDB().dq("select characterid from characters where owner=? and retired=0", a.getId());
		Set<Char> results = new TreeSet<>();
		for (ResultsRow r : rows) {
			results.add(Char.get(r.getInt()));
		}
		return results;
	}

	@Nullable
	public static Char getMostRecent(@Nonnull User avatar) {
		Results results=GPHUD.getDB().dq("select characterid from characters where owner=? and retired=0 order by lastactive desc limit 0,1",avatar.getId());
		if (results.empty()) { return null; }
		try { return Char.get(results.iterator().next().getInt("characterid")); }
		catch (Exception e) { // weird
			GPHUD.getLogger().log(SEVERE,"Exception while instansiating most recently used character?",e);
			return null;
		}
	}
	@Nullable
	public static Char getMostRecent(@Nonnull User avatar, @Nullable Instance optionalinstance) {
		if (optionalinstance==null) { return getMostRecent(avatar); }
		Results results=GPHUD.getDB().dq("select characterid from characters where owner=? and retired=0 and instanceid=? order by lastactive desc limit 0,1",avatar.getId(),optionalinstance.getId());
		if (results.empty()) { return null; }
		try { return Char.get(results.iterator().next().getInt("characterid")); }
		catch (Exception e) { // weird
			GPHUD.getLogger().log(SEVERE,"Exception while instansiating most recently used character?",e);
			return null;
		}
	}

	@Nonnull
	public static DropDownList getNPCList(@Nonnull State st, String listname) {
		DropDownList list=new DropDownList(listname);
		for (ResultsRow row:GPHUD.getDB().dq("select characterid,name from characters where instanceid=? and owner=?",st.getInstance().getId(),User.getSystem().getId())) {
			list.add(row.getInt("characterid")+"",row.getString("name"));
		}
		return list;
	}

	/**
	 * Gets the characters personal URL
	 *
	 * @return The URL, or null
	 */
	public String getURL() {
		return getString("url");
	}

	/**
	 * Sets the characters callback URL - call me often!.
	 * Only updates the database if the URL has changed.
	 * Also updates the "Last accessed" time if its more than 60 seconds out of date.
	 * Sends a shutdown hudMessage to the old URL if this replaces it.
	 *
	 * @param url URL to set to
	 */
	public void setURL(@Nonnull String url) {
		String oldurl = getURL();
		int now = getUnixTime();

		// update last used timer if we're the same URL and its more than 60 seconds since the last timer and we're done
		if (oldurl != null && oldurl.equals(url)) {
			refreshURL(url);
			return;
		}

		// if there was a URL, send it a shutdown
		if (oldurl != null && !("".equals(oldurl))) {
			JSONObject shutdown = new JSONObject().put("incommand", "shutdown").put("shutdown", "Connection replaced by new character connection");
			Transmission t = new Transmission(this, shutdown, oldurl);
			t.start();
		}
		// set the URL
		if (url.startsWith("https")) { MailTools.logTrace("HTTPS URL violation",url); }
		d("update characters set url=?, lastactive=?, urllast=?, urlfirst=?, authnode=? where characterid=?", url, now, now, now, Interface.getNode(), getId());

	}

	/**
	 * Gets the last active time for the URL.
	 *
	 * @return Unix format timestamp for the last use of this URL - accurate to within REFRESH_INTERVAL
	 */
	public Integer getURLLast() {
		return getInt("urllast");
	}

	/**
	 * Get the instance for this character.
	 *
	 * @return The Instance
	 */
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}

	/**
	 * Get the owning avatar for this character.
	 *
	 * @return Avatar owner of this character
	 */
	public User getOwner() {
		return User.get(getInt("owner"));
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
	 * @return Sum of the entries
	 */
	public int sumPool(@Nonnull Pool p) {
		Integer sum = dqi(true, "select sum(adjustment) from characterpools where characterid=? and poolname like ?", getId(), p.fullName());
		if (sum == null) { return 0; }
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
	public void addPool(@Nonnull State st, @Nonnull Pool p, int adjustment, String description) {
		d("insert into characterpools(characterid,poolname,adjustment,adjustedbycharacter,adjustedbyavatar,description,timedate) values(?,?,?,?,?,?,?)", getId(), p.fullName(), adjustment, st.getCharacter().getId(), st.avatar().getId(), description, getUnixTime());
	}

	/**
	 * Add an adjustment to a pool, as an administrator (Avatar).
	 *
	 * @param st          State of the grantor (requires avatar)
	 * @param p           Pool
	 * @param adjustment  Ammount to grant
	 * @param description Audit logged description
	 */
	public void addPoolAdmin(@Nonnull State st, @Nonnull Pool p, int adjustment, String description) {
		d("insert into characterpools(characterid,poolname,adjustment,adjustedbycharacter,adjustedbyavatar,description,timedate) values(?,?,?,?,?,?,?)", getId(), p.fullName(), adjustment, null, st.avatar().getId(), description, getUnixTime());
	}

	/**
	 * Add an adjustment to a pool on behalf of SYSTEM.
	 *
	 * @param st          State (not used)
	 * @param p           Pool
	 * @param adjustment  Ammount to adjust
	 * @param description Logged reason for the change
	 */
	public void addPoolSystem(State st, @Nonnull Pool p, int adjustment, String description) {
		d("insert into characterpools(characterid,poolname,adjustment,adjustedbycharacter,adjustedbyavatar,description,timedate) values(?,?,?,?,?,?,?)", getId(), p.fullName(), adjustment, null, User.getSystem().getId(), description, getUnixTime());
	}

	/**
	 * Sum visit time on sim.
	 *
	 * @param since Ignore visits that end before this time (Unix Time)
	 * @return Total number of seconds the character has visited the sim for since the specified time
	 */
	public int sumVisits(int since) {
		int now = getUnixTime();
		Results visits = dq("select starttime,endtime from visits where characterid=? and (endtime is null or endtime>?)", getId(), since);
		int seconds = 0;
		for (ResultsRow r : visits) {
			Integer end = r.getInt("endtime");
			Integer start = r.getInt("starttime");
			if (end == null) { end = now; }
			if (start != null) { seconds = seconds + (end - start); }
		}
		return seconds;
	}

	/**
	 * Sum a pool since a given time
	 *
	 * @param p     Pool
	 * @param since Unix Time to count points since
	 * @return Number of points in the given period.
	 */
	public int sumPoolSince(@Nullable Pool p, int since) {
		if (p == null) { throw new SystemException("Null pool not permitted"); }
		Integer sum = dqi(true, "select sum(adjustment) from characterpools where characterid=? and poolname like ? and timedate>=?", getId(), p.fullName(), since);
		if (sum == null) { return 0; }
		return sum;
	}

	/**
	 * Sum a pool since a given number of days
	 *
	 * @param p    Pool
	 * @param days Number of days ago to start counting from.
	 * @return Number of points in the pool in the selected time range.
	 */
	public int sumPoolDays(Pool p, float days) {
		int seconds = (int) (days * 60.0 * 60.0 * 24.0);
		return sumPoolSince(p, getUnixTime() - seconds);
	}

	/**
	 * Call a characters HUD to get a radar list of nearby Characters.
	 *
	 * @param st State
	 * @return List of nearby Chars
	 */
	@Nonnull
	public List<Char> getNearbyCharacters(@Nonnull State st) {
		Char character = st.getCharacter();
		boolean debug = false;
		List<Char> chars = new ArrayList<>();
		String uri = character.getURL();
		if (uri == null || uri.isEmpty()) {
			throw new UserException("Your character does not have a valid in-world presence");
		}
		JSONObject radarrequest = new JSONObject().put("incommand", "radar");
		Transmission t = new Transmission(this, radarrequest);
		//noinspection CallToThreadRun
		t.run();
		JSONObject j = t.getResponse();
		if (j == null) { throw new SystemException("Failed to get a useful response from the remote HUD"); }
		String avatars = j.optString("avatars", "");
		if (avatars == null || avatars.isEmpty()) {
			throw new UserException("Sorry, you are not near any other avatars");
		}
		for (String key : avatars.split(",")) {
			User a = User.findOptional(key);
			if (a != null) {
				Char c = null;
				try { c = Char.getActive(a, st.getInstance()); } catch (UserException e) {
				}
				if (c != null) { chars.add(c); }
			}
		}
		return chars;
	}

	/**
	 * Gets the level for this character.
	 *
	 * @param st State
	 * @return Level number
	 */
	public int getLevel(@Nonnull State st) {
		if (st.hasModule("Experience")) { return Experience.toLevel(st, Experience.getExperience(st, this)); }
		return 0;
	}

	/**
	 * Get the last played time for this character.
	 *
	 * @return Unix time the character was last played.
	 */
	public Integer getLastPlayed() {
		return dqi(true, "select lastactive from characters where characterid=?", getId());
	}

	/**
	 * Get all the pools this character has.
	 *
	 * @param st State
	 * @return List of Pools
	 */
	@Nonnull
	public Set<Pool> getPools(@Nonnull State st) {
		Set<Pool> pools = new TreeSet<>();
		Results results = dq("select distinct poolname from characterpools where characterid=?", getId());
		for (ResultsRow r : results) {
			String name = r.getString();
			if (st.hasModule(name)) {
				Pool p = Modules.getPool(st, name);
				pools.add(p);
			}
		}
		return pools;
	}

	/**
	 * Get the character group of a given type.
	 *
	 * @param grouptype Group type string
	 * @return The CharacterGroup or null
	 */
	@Nullable
	public CharacterGroup getGroup(String grouptype) {
		Integer group = dqi(false, "select charactergroups.charactergroupid from charactergroups inner join charactergroupmembers on charactergroups.charactergroupid=charactergroupmembers.charactergroupid where characterid=? and charactergroups.type=?", getId(), grouptype);
		if (group == null) { return null; }
		return CharacterGroup.get(group);
	}

	/**
	 * Calculate the next free point time string for a pool.
	 *
	 * @param pool  Pool
	 * @param maxxp Maximum ammount of XP earnable in a period
	 * @param days  Period (days)
	 * @return Explanation of when the next point is available.
	 */
	@Nonnull
	public String poolNextFree(@Nonnull Pool pool, int maxxp, float days) {
		if (maxxp == 0) { return "NEVER"; }
		int now = getUnixTime();
		int nextfree = poolNextFreeAt(pool, maxxp, days);
		if (now >= nextfree) { return "NOW"; }

		int duration = nextfree - now;
		return "in " + duration(duration, false);
	}

	/**
	 * Calculate the date-time of the next free point for a pool.
	 *
	 * @param pool  Pool
	 * @param maxxp Maximum ammount of XP in a period
	 * @param days  Period in days
	 * @return Date-time of the point of next free (may be in the past, in which case available NOW).
	 */
	public int poolNextFreeAt(@Nonnull Pool pool, int maxxp, float days) {
		boolean debug = false;
		int now = getUnixTime();
		int since = (int) (now - (days * 60 * 60 * 24));
		Results res = dq("select adjustment,timedate from characterpools where characterid=? and poolname=? and timedate>?", getId(), pool.fullName(), since);
		int awarded = 0;
		Map<Integer, Integer> when = new TreeMap<>(); // map time stamps to award.
		for (ResultsRow r : res) {
			int ammount = r.getInt("adjustment");
			int at = r.getInt("timedate");
			awarded += ammount;
			while (when.containsKey(at)) { at++; }
			when.put(at, ammount);
		}
		int overshoot = awarded - maxxp;
		if (overshoot < 0) { return now; }
		int datefilled = 0;
		for (Map.Entry<Integer, Integer> entry : when.entrySet()) {
			int ammount = entry.getValue();
			overshoot -= ammount;
			if (overshoot < 0) {
				return (int) (entry.getKey() + (days * 60 * 60 * 24));
			}
		}
		return now;
	}

	/**
	 * Push a JSON K:V pair to the characters hud.
	 *
	 * @param key   Key
	 * @param value Value
	 */
	public void push(@Nonnull String key, String value) {
		String url = getURL();
		if (url == null) { return; }
		JSONObject j = new JSONObject();
		j.put(key, value);
		Transmission t = new Transmission(this, j);
		t.start();
	}

	/**
	 * Push a HUD Wearer (ownersay) hudMessage to the hud
	 *
	 * @param message Text hudMessage to send.
	 */
	public void hudMessage(String message) {
		push("message", message);
	}

	/**
	 * Count the number of queued/offline messages available to this user.
	 *
	 * @return Message count
	 */
	public int messages() { return Message.count(this); }

	/**
	 * Push the hudMessage count to the client's HUD.
	 */
	public void pushMessageCount() { push("messagecount", messages() + ""); }

	/**
	 * Log a queued/offline queueMessage
	 *
	 * @param message         Message to send (JSON format)
	 * @param lifespanseconds life span of the queueMessage in seconds
	 */
	public void queueMessage(@Nonnull JSONObject message, int lifespanseconds) {
		Message.add(this, getUnixTime() + lifespanseconds, message);
		pushMessageCount();
	}

	/**
	 * get the next queued message and marks it as active
	 *
	 * @return Message object
	 */
	@Nullable
	public Message getMessage() {
		return Message.getNextMessage(this);
	}

	/**
	 * gets the currently active message
	 *
	 * @return Message object
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
		Integer id = dqi(false, "select zoneid from characters where characterid=?", getId());
		if (id == null) { return null; }
		return Zone.get(id);
	}

	/**
	 * Set the zone this character is in
	 *
	 * @param zone Zone
	 */
	public void setZone(@Nullable Zone zone) {
		Integer id = null;
		if (zone != null) { id = zone.getId(); }
		if (id == null) {
			d("update characters set zoneid=null where characterid=?", getId());
			return;
		}
		d("update characters set zoneid=? where characterid=?", id, getId());
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
		Set<CharacterGroup> ret = new TreeSet<>();
		for (ResultsRow r : dq("select charactergroupid from charactergroupmembers where characterid=?", getId())) {
			ret.add(CharacterGroup.get(r.getInt()));
		}
		return ret;
	}

	/**
	 * Used to load a list of conveyances
	 *
	 * @param st State
	 * @return Set of conveyanced KVs
	 */
	@Nonnull
	private Set<KV> getConveyances(@Nonnull State st) {
		// load the previously sent conveyances from the DB.  Note that the 'state' caches these queries so its not quite as garbage as it sounds.
		validate(st);
		Set<KV> conveyances = new TreeSet<>();
		for (KV kv : Modules.getKVSet(st)) {
			if (kv != null && kv.conveyas() != null && !kv.conveyas().isEmpty()) {
				conveyances.add(kv);
			}
		}
		return conveyances;
	}

	/**
	 * Copy the existing conveyance values from their cached KVs into a map.
	 *
	 * @param st State
	 * @return Map of KV=Values for all conveyanced data.
	 */
	@Nonnull
	private Map<KV, String> loadConveyances(@Nonnull State st) {
		// load the previously sent conveyances from the DB.  Note that the 'state' caches these queries so its not quite as garbage as it sounds.
		validate(st);
		Map<KV, String> conveyances = new TreeMap<>();
		for (KV kv : getConveyances(st)) {
			try {
				conveyances.put(kv, st.getKV("gphudclient.conveyance-" + kv.conveyas()).value());
			} catch (SystemException e) {
				st.logger().log(SEVERE, "Exceptioned loading conveyance " + kv.conveyas() + " in instance " + st.getInstanceString() + " - " + e.getLocalizedMessage());
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
	public void initialConveyances(@Nonnull State st, @Nonnull JSONObject payload) {
		boolean debug = false;
		validate(st);
		Map<KV, String> oldconveyances = loadConveyances(st);
		for (Map.Entry<KV, String> entry : oldconveyances.entrySet()) {
			KV kv = entry.getKey();
			String oldvalue = entry.getValue();
			String newvalue = st.getKV(kv.fullname()).value();
			payload.put(kv.conveyas(), newvalue); // always put in init
			if (!oldvalue.equals(newvalue)) {
				this.setKV(st, "gphudclient.conveyance-" + kv.conveyas(), newvalue); // skip cache flush
			}
		}
	}

	/**
	 * Append any conveyances that have changed to the payload
	 *
	 * @param st      State
	 * @param payload Message to append changed conveyances to.
	 */
	public void appendConveyance(@Nonnull State st, @Nonnull JSONObject payload) {
		boolean debug = false;
		validate(st);
		Map<KV, String> oldconveyances = loadConveyances(st);
		for (Map.Entry<KV, String> entry : oldconveyances.entrySet()) {
			KV kv = entry.getKey();
			String oldvalue = entry.getValue();
			String newvalue = st.getKV(kv.fullname()).value();
			if (!oldvalue.equals(newvalue)) {
				payload.put(kv.conveyas(), newvalue);
				this.setKV(st, "gphudclient.conveyance-" + kv.conveyas(), newvalue); // skip cache update/flush
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
		Integer region = getInt("regionid");
		if (region == null) { return null; }
		Region r=Region.get(region,true);
		if (r.isRetired()) { return null; }
		return r;
	}

	/**
	 * Set the current region for this character
	 *
	 * @param r Region
	 */
	public void setRegion(@Nonnull Region r) {
		//System.out.println("Setting region to "+r+" for "+getName()+" where it is currently "+getRegion());
		if (getRegion()!=r) {
			set("regionid", r.getId());
		}
	}

	/**
	 * Select the played by avatar for this character.
	 *
	 * @return Avatar
	 */
	@Nullable
	public User getPlayedBy() {
		Integer avatarid = dqi(true, "select playedby from characters where characterid=?", getId());
		if (avatarid == null) { return null; }
		return User.get(avatarid);
	}

	/**
	 * Update the "is being played by" field on the character sheet
	 *
	 * @param avatar Avatar who is playing this character.
	 */
	public void setPlayedBy(@Nonnull User avatar) {
		set("playedby", avatar.getId());
	}

	public void validate(@Nonnull State st) throws SystemException {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getInstance()) { throw new SystemException("Char / State Instance mismatch"); }
	}

	protected int getNameCacheTime() { return 5; } // characters /may/ be renamable, just not really sure at this point

	public void retire() {
		if (retired()) { return; }
		String now = new SimpleDateFormat("yyyyMMdd").format(new Date());
		rename(getName() + " (Retired " + now + ")");
		set("retired", true);
	}

	public boolean retired() {
		return getBool("retired");
	}

	public void rename(String newname) {
		Integer count = dqi(true, "select count(*) from characters where name like ? and instanceid=?", newname, getInstance().getId());
		if (count != 0) {
			throw new UserException("Unable to rename character '" + getName() + "' to '" + newname + "', that name is already taken.");
		}
		set("name", newname);
	}

	public void closeVisits(@Nonnull State st) {
		if (st.getInstance()!=getInstance()) { throw new IllegalStateException("State character instanceid mismatch"); }
		d("update visits set endtime=UNIX_TIMESTAMP() where characterid=? and regionid=? and endtime is null",st.getCharacter().getId(),st.getRegion().getId());
	}
	public void closeURL(@Nonnull State st) {
		if (st.getInstance()!=getInstance()) { throw new IllegalStateException("State character instanceid mismatch"); }
		d("update characters set url=null,urlfirst=null,urllast=null,authnode=null,zoneid=null,regionid=null,lastactive=UNIX_TIMESTAMP(),playedby=null where characterid=?",st.getCharacter().getId());
	}

	/** Called when a ping to the URL completes, update the timer */
	public void pinged() {
		d("update characters set urllast=? where characterid=?", UnixTime.getUnixTime(),getId());
	}

	public boolean isOnline() {
		String s=getURL();
		if (s==null || s.isEmpty()) { return false; }
		return true;
	}

	public void setOwner(@Nonnull User newowner) {
		set("owner",newowner.getId());
		// purge any primary characters referring to this
		PrimaryCharacters.purge(this);
	}

	public void considerPushingConveyances() {
		JSONObject json=new JSONObject();
		appendConveyance(new State(this),json);
		//System.out.println("Consider pushing: "+json.toString()+" = "+json.keySet().size());
		if (json.keySet().size()>0) {
			new Transmission(this,json).start();
		}
	}
}
