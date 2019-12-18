package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.User.UserRemoteFailureException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Experience.VisitXP;
import net.coagulate.GPHUD.Modules.Zoning.ZoneTransport;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static net.coagulate.Core.Tools.UnixTime.*;

/**
 * Reference to a region, which is connected to an instance.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Region extends TableRow {

	protected Region(final int id) { super(id); }

	/**
	 * Update the last used time of a URL.
	 *
	 * @param url URL to refresh
	 */
	public static void refreshURL(final String url) {
		final String t = "regions";
		final int refreshifolderthan = UnixTime.getUnixTime() - TableRow.REFRESH_INTERVAL;
		final int toupdate = GPHUD.getDB().dqinn( "select count(*) from " + t + " where url=? and urllast<?", url, refreshifolderthan);
		if (toupdate == 0) { return; }
		if (toupdate > 1) {
			GPHUD.getLogger().warning("Unexpected anomoly, " + toupdate + " rows to update on " + t + " url " + url);
		}
		//Log.log(Log.DEBUG,"SYSTEM","DB_Region","Refreshing REGION url "+url);
		GPHUD.getDB().d("update " + t + " set urllast=?,authnode=? where url=?", UnixTime.getUnixTime(), Interface.getNode(), url);
	}

	static void wipeKV(@Nonnull final Instance instance, final String key) {
		final String kvtable = "regionkvstore";
		final String maintable = "regions";
		final String idcolumn = "regionid";
		GPHUD.getDB().d("delete from " + kvtable + " using " + kvtable + "," + maintable + " where " + kvtable + ".k like ? and " + kvtable + "." + idcolumn + "=" + maintable + "." + idcolumn + " and " + maintable + ".instanceid=?", key, instance.getId());
	}

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A Region representation
	 */
	@Nonnull
	public static Region get(final int id, final boolean allowretired) {
		final Region r= (Region) factoryPut("Region", id, new Region(id));
		if (r.isRetired() && (!allowretired)) {
			final UserException exception=new UserInputStateException("Attempt to access retired region");
			GPHUD.getLogger("Regions").log(WARNING,"Attempt to access retired region",exception);
			throw exception;
		}
		return r;
	}


	public boolean isRetired() {
		return getBool("retired");
	}
	/**
	 * Find a region by name.
	 *
	 * @param name Name of region to locate
	 * @return Region object for that region, or null if none is found.
	 */
	@Nullable
	public static Region findNullable(final String name, final boolean allowretired) {
		try {
			final int regionid = GPHUD.getDB().dqinn("select regionid from regions where name=?", name);
			return get(regionid, allowretired);
		} catch (@Nonnull final NoDataException e) { return null; }
	}

	@Nonnull
	public static Region find(final String name, final boolean allowretired) {
		final Region r=findNullable(name,allowretired);
		if (r==null) { throw new UserInputLookupFailureException("No active region named '"+name+"' found"); }
		return r;
	}

	/**
	 * Register a region against an instance.
	 *
	 * @param region Name of region to register
	 * @param i      Instance object to register the region with
	 * @return A blank string on success, or a text hudMessage explaining any problem.
	 */
	@Nonnull
	public static String joinInstance(final String region, @Nonnull final Instance i) {
		// TO DO - lacks validation
		final int exists = GPHUD.getDB().dqinn( "select count(*) from regions where name=?", region);
		if (exists == 0) {
			GPHUD.getLogger().info("Joined region '" + region + "' to instance " + i);
			GPHUD.getDB().d("insert into regions(name,instanceid) values(?,?)", region, i.getId());
			return "";
		}
		return "Region is already registered!";
	}

	@Nonnull
	@Override
	public String getLinkTarget() { return "regions"; }

	/**
	 * Gets the instance associated with this region
	 *
	 * @return The Instance object
	 */
	@Nonnull
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "regions";
	}

	@Nonnull
	@Override
	public String getIdField() {
		return "regionid";
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}

	/**
	 * Gets the URL associated with this region's server
	 *
	 * @return URL
	 */
	@Nullable
	public String getURLNullable() {
		return getStringNullable("url");
	}

	/**
	 * Gets the URL associated with this region's server
	 *
	 * @return
	 */
	@Nonnull
	public String getURL() {
		final String url=getURLNullable();
		if (url == null) {
			throw new UserRemoteFailureException("This region has no callback URL");
		}
		return url;
	}

	/**
	 * Sets the region's URL to callback to the server - CALL THIS FREQUENTLY.
	 * This does not /just/ set the region's url, it checks if it needs to be set firstly, it also updates it if and only if necessary, along with shutting down the URL it replaced.
	 * It also updates the "urllast used" timer if it's more than 60 seconds old.  (used for 'is alive' and 'ping' checking etc etc).
	 *
	 * @param url Targets URL
	 */
	public void setURL(final String url) {
		String oldurl = null;
		try { oldurl = getURL(); } catch (@Nonnull final UserException e) {} // should only mean there was a null URL
		final int now = getUnixTime();

		if (oldurl != null && oldurl.equals(url)) {
			if ((now - getURLLast()) > 60) {
				d("update regions set urllast=?,authnode=? where regionid=?", now, Interface.getNode(), getId());
			}
			return;
		}

		if (oldurl != null && !("".equals(oldurl))) {
			GPHUD.getLogger().info("Sending shutdown to old URL : " + oldurl);
			final JSONObject tx = new JSONObject().put("incommand", "shutdown").put("shutdown", "Connection replaced by new region server");
			final Transmission t = new Transmission(this, tx, oldurl);
			t.start();
		}

		d("update regions set url=?, urllast=?, authnode=? where regionid=?", url, now, Interface.getNode(), getId());

	}

	/**
	 * Gets the UNIX time the region's server last checked in.
	 *
	 * @return UnixTime the last time this server's url was refreshed / used
	 */
	@Nonnull
	public Integer getURLLast() {
		return getInt("urllast");
	}

	/**
	 * Used for region visitation checking.
	 * Send a complete list of Avatar Names for the region.  Avatars with a visit not in the data passed will be assumed to have left.
	 * Avatars in the passed set but without a visit will NOT Be registered as this requires binding to a character, assumed the HUD will do this 'shortly'.
	 *
	 * @param avatarsarray Array of ALL avatar names in the region.
	 */
	public void verifyAvatars(final String[] avatarsarray) {
		final String report = "";
		final Set<String> avatars = new HashSet<>(Arrays.asList(avatarsarray));
		final Results db = dq("select avatarid from visits where regionid=? and endtime is null", getId());
		// iterate over the current visits
		for (final ResultsRow row : db) {
			final int avatarid = row.getInt("avatarid");
			final String name = User.get(avatarid).getName();
			// make sure those visits are in the list of avatars
			if (avatars.contains(name)) {
				avatars.remove(name); // matches an avatar on the sim.
			} else {
				// doesn't match an avatar on the sim
				GPHUD.getLogger().warning("Avatar " + name + " not on sim but visiting in GPHUD.getDB().  Marking as left");
				d("update visits set endtime=? where regionid=? and avatarid=? and endtime is null", UnixTime.getUnixTime(), getId(), avatarid);
			}
		}
		// whatever is left in the set isn't logged in the db yet.  we dont care but...
		for (final String s : avatars) {
			GPHUD.getLogger().info("Avatar " + s + " is present on sim but not in visits DB, hopefully they'll register soon.");
		}
	}

	/**
	 * Update the visits noting the following avatars left the sim.
	 *
	 * @param st      State
	 * @param avatars List of Avatar UUIDs or Names that have left the sim.
	 */
	public void departingAvatars(@Nonnull final State st, @Nonnull final Set<User> avatars) {
		final boolean debug = false;
		for (final User avatar : avatars) {
			// for all the departing avatars
			try {
				final int avatarid = avatar.getId();
				// if the avatar exists, see if there's a visit
				final Results rows = dq("select characterid from visits where avatarid=? and regionid=? and endtime is null", avatarid, getId());
				final int count = rows.size();
				if (count > 0) {
					st.logger().info("Disconnected avatar " + avatar.getName());
					d("update visits set endtime=? where endtime is null and regionid=? and avatarid=?", UnixTime.getUnixTime(), getId(), avatarid);
				}
				// computer visit XP ((TODO REFACTOR ME?))
				for (final ResultsRow r : rows) {
					final State temp = new State();
					temp.setInstance(st.getInstance());
					temp.setCharacter(Char.get(r.getInt("characterid")));
					new VisitXP(-1).runAwards(st, temp.getCharacter());
				}
				final int instanceid = getInstance().getId();
				final Results urls = dq("select url from characters where instanceid=? and playedby=? and url is not null", instanceid, avatarid);
				// if the visitor (character) has URLs send them a ping, which will probably 404 and remove its self
				for (final ResultsRow row : urls) {
					final String url = row.getStringNullable("url");
					final JSONObject ping = new JSONObject().put("incommand", "ping");
					final Transmission t = new Transmission(null, ping, url, 5);
					t.start();
				}
			} catch (@Nonnull final Exception e) {
				st.logger().log(SEVERE, "Exception in departingAvatars", e);
			}
		}
	}

	/**
	 * Log a product's version information.
	 *
	 * @param st          State
	 * @param type        Type of product (hud, server)
	 * @param version     Version string (XX.YY.ZZ format) NOTE XX/YY/ZZ should not exceeed 2 digits (as it's stored literally as XXYYZZ integer)
	 * @param versiondate Parsable date (see FireStorm preprocessor macro __DATE__)
	 * @param versiontime Parsable time (see FireStorm preprocessor macro __TIME__)
	 */
	public void recordVersion(@Nonnull final State st, final String type, @Nonnull final String version, final String versiondate, final String versiontime) {
		final Date d;
		try {
			final SimpleDateFormat df = new SimpleDateFormat("MMM d yyyy HH:mm:ss");
			df.setLenient(true);
			String datetime = versiondate + " " + versiontime;
			datetime = datetime.replaceAll(" {2}", " ");
			d = df.parse(datetime);
		} catch (@Nonnull final ParseException ex) {
			throw new SystemImplementationException("Failed to parse date time from " + versiondate + " " + versiontime, ex);
		}
		final ResultsRow regiondata = dqone( "select region" + type + "version,region" + type + "datetime from regions where regionid=?", getId());
		final Integer oldversion = regiondata.getIntNullable("region" + type + "version");
		final Integer olddatetime = regiondata.getIntNullable("region" + type + "datetime");
		final int newversion = Interface.convertVersion(version);
		final int newdatetime = (int) (d.getTime() / 1000.0);
		if (oldversion == null || olddatetime == null || olddatetime < newdatetime || oldversion < newversion) {
			d("update regions set region" + type + "version=?,region" + type + "datetime=? where regionid=?", newversion, newdatetime, getId());
			final String olddesc = formatVersion(oldversion, olddatetime, false);
			final String newdesc = formatVersion(newversion, newdatetime, false);
			st.logger().info("Version upgrade of " + type + " from " + olddesc + " to " + newdesc);
			final State fake = new State();
			fake.setInstance(st.getInstance());
			fake.setAvatar(User.getSystem());
			Audit.audit(fake, Audit.OPERATOR.AVATAR, null, null, "Upgrade", type, olddesc, newdesc, "Product version upgraded");
		}
	}

	/**
	 * Internal method to reconstruct a human readable version/datetime string for this region's versions.
	 *
	 * @param version  Version number, XXYYZZ (XX.YY.ZZ where XX*10000+YY*100+ZZ)
	 * @param datetime Unix DateTime stamp of the version
	 * @param html     To HTML or not
	 * @return String form of the version information passed
	 */
	@Nonnull
	private String formatVersion(@Nullable final Integer version, @Nullable final Integer datetime, final boolean html) {
		String v = "";
		final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		if (version == null) { v += "v??? "; } else {
			v = v + "v" + (version / 10000) + "." + ((version / 100) % 100) + "." + (version % 100) + " ";
		}
		if (datetime == null) { v += "???"; } else {
			if (html) { v += "<i>( "; }
			v += df.format(new Date((long) (1000.0 * datetime)));
			if (html) { v += " )</i>"; }
		}
		return v;
	}

	/**
	 * Convenience method that logs a HUD version
	 *
	 * @param st          State
	 * @param version     Version "XX.YY.ZZ"
	 * @param versiondate Preprocessor macro _DATE_ in Firestorm
	 * @param versiontime Preprocessor macro _TIME_ in Firestorm
	 */
	public void recordHUDVersion(@Nonnull final State st, @Nonnull final String version, final String versiondate, final String versiontime) {
		recordVersion(st, "hud", version, versiondate, versiontime);
	}

	/**
	 * Convenience method that logs a Server version
	 *
	 * @param st          State
	 * @param version     Version "XX.YY.ZZ"
	 * @param versiondate Preprocessor macro _DATE_
	 * @param versiontime Preprocessor macro _TIME_
	 */
	public void recordServerVersion(@Nonnull final State st, @Nonnull final String version, final String versiondate, final String versiontime) {
		recordVersion(st, "server", version, versiondate, versiontime);
	}

	/**
	 * Return the open visits to this region.
	 *
	 * @return Set of Characters currently visiting this region.
	 */
	@Nonnull
	public Set<Char> getOpenVisits() {
		final Set<Char> characters = new TreeSet<>();
		final Results results = dq("select characterid from visits where regionid=? and endtime is null", getId());
		for (final ResultsRow r : results) {
			characters.add(Char.get(r.getInt("characterid")));
		}
		return characters;
	}
	@Nonnull
	public Set<User> getAvatarOpenVisits() {
		final Set<User> users = new HashSet<>();
		final Results results = dq("select avatarid from visits where regionid=? and endtime is null", getId());
		for (final ResultsRow r : results) {
			try { users.add(User.get(r.getInt("avatarid"))); }
			catch (@Nonnull final Exception e) {}
		}
		return users;
	}

	/**
	 * Return the online/offline status of this region as a string
	 *
	 * @return String, starts with OFFLINE or STALLED if problematic, otherwise "Online"
	 */
	@Nonnull
	public String getOnlineStatus(final String timezone) {
		final int urllast = getURLLast();
		if (isRetired()) { return "Retired"; }
		if (urllast == 0) { return "OFFLINE forever?"; }
		if (getURLNullable() == null || getURLNullable().isEmpty()) {
			return "OFFLINE for " + duration(getUnixTime() - urllast);
		}
		final String authnode = getAuthNode();
		if ((getUnixTime() - urllast) > (15 * 60)) {
			return "STALLED at " + fromUnixTime(urllast, timezone) + " via server " + authnode;
		}
		return "Online at " + fromUnixTime(urllast, timezone) + " via server " + authnode;
	}

	/**
	 * Return the node name (host name) that this region server is using as its primary contact point.
	 *
	 * @return Short name of the backend LSLR server node in use
	 */
	@Nullable
	public String getAuthNode() {
		return dqs( "select authnode from regions where regionid=?", getId());
	}

	/**
	 * Extract the currently known server version.
	 *
	 * @param html As HTML?
	 * @return Server version string
	 */
	@Nonnull
	public String getServerVersion(final boolean html) {
		final ResultsRow r = dqone( "select regionserverversion,regionserverdatetime from regions where regionid=?", getId());
		return formatVersion(r.getIntNullable("regionserverversion"), r.getIntNullable("regionserverdatetime"), html);
	}

	/**
	 * Extract the currently known HUD version.
	 *
	 * @param html As HTML?
	 * @return HUD version string
	 */
	@Nonnull
	public String getHUDVersion(final boolean html) {
		final ResultsRow r = dqone( "select regionhudversion,regionhuddatetime from regions where regionid=?", getId());
		return formatVersion(r.getIntNullable("regionhudversion"), r.getIntNullable("regionhuddatetime"), html);
	}

	/**
	 * Figure out if this region is running needs to update to the latest version of the server/hud package.
	 *
	 * @return True if the region requires an update, false otherwise
	 */
	public boolean needsUpdate() {
		final Integer ourserver = dqi( "select regionserverversion from regions where regionid=?", getId());
		final Integer maxserver = dqi( "select MAX(regionserverversion) from regions");
		if (ourserver != null && maxserver != null) {
			if (maxserver > ourserver) { return true; }
		}
		final Integer ourhud = dqi( "select regionhudversion from regions where regionid=?", getId());
		final Integer maxhud = dqi( "select MAX(regionhudversion) from regions");
		if (ourhud != null && maxhud != null) {
			if (maxhud > ourhud) { return true; }
		}
		return false;
	}

	/**
	 * Push a message to this region's server
	 *
	 * @param json JSON Message to send
	 */
	public void sendServer(final JSONObject json) {
		new Transmission(this, json).start();
	}
	public void sendServerSync(final JSONObject json) {
		final Transmission t=new Transmission(this, json);
		//noinspection CallToThreadRun
		t.run();
		if (t.failed()) { throw new UserRemoteFailureException("Connection to server failed"); }
	}

	/**
	 * Get all zones that are present in this region.
	 *
	 * @return Set of Zone objects for this region.
	 */
	@Nonnull
	public Set<Zone> getZones() {
		final Set<Zone> zones = new TreeSet<>();
		for (final ResultsRow r : dq("select distinct zoneid from zoneareas where regionid=?", getId())) {
			zones.add(Zone.get(r.getInt()));
		}
		return zones;
	}

	/**
	 * Broadcast the new zoning for this region via the region server
	 */
	public void pushZoning() {
		final JSONObject j = new JSONObject();
		j.put("incommand", "broadcast");
		j.put("zoning", ZoneTransport.createZoneTransport(this));
		final Transmission t = new Transmission(this, j);
		t.start();
	}

	@Nonnull
	@Override
	public String getKVTable() {
		return "regionkvstore";
	}

	@Nonnull
	@Override
	public String getKVIdField() {
		return "regionid";
	}

	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getInstance()) { throw new SystemConsistencyException("Region / State Instance mismatch"); }
	}

	protected int getNameCacheTime() { return 60 * 60; } // this name doesn't change, cache 1 hour

	@Nonnull
	public Integer getOpenVisitCount() {
		return dqinn( "select count(*) from visits where endtime is null and regionid=?", getId());
	}

	public void setGlobalCoordinates(final int x, final int y) {
		d("update regions set regionx=?,regiony=? where regionid=?",x,y,getId());
	}



	@Nonnull
	public String getGlobalCoordinates() {
		final ResultsRow r = dqone( "select regionx,regiony from regions where regionid=?", getId());
		final Integer x=r.getIntNullable("regionx");
		final Integer y=r.getIntNullable("regiony");
		if (x==null || y==null) { throw new UserRemoteFailureException("Unable to extract "+getNameSafe()+"'s global co-ordinates.  Try '*reboot'ing the region server"); }
		return "<"+x+","+y+",0>";
	}
    
    /*protected void delete() {
        d("delete from regions where regionid=?",getId());
        Log.log(Log.CRIT, getInstance().getName()+"/"+getName(), "Region", "Deleting region "+getName());
    }*/
}
