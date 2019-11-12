package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Experience.VisitXP;
import net.coagulate.GPHUD.Modules.Zoning.ZoneTransport;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.logging.Level.SEVERE;
import static net.coagulate.Core.Tools.UnixTime.*;

/**
 * Reference to a region, which is connected to an instance.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Region extends TableRow {

	protected Region(int id) { super(id); }

	/**
	 * Update the last used time of a URL.
	 *
	 * @param url URL to refresh
	 */
	public static void refreshURL(String url) {
		String t = "regions";
		int refreshifolderthan = UnixTime.getUnixTime() - TableRow.REFRESH_INTERVAL;
		int toupdate = GPHUD.getDB().dqi(true, "select count(*) from " + t + " where url=? and urllast<?", url, refreshifolderthan);
		if (toupdate == 0) { return; }
		if (toupdate > 1) {
			GPHUD.getLogger().warning("Unexpected anomoly, " + toupdate + " rows to update on " + t + " url " + url);
		}
		//Log.log(Log.DEBUG,"SYSTEM","DB_Region","Refreshing REGION url "+url);
		GPHUD.getDB().d("update " + t + " set urllast=?,authnode=? where url=?", UnixTime.getUnixTime(), Interface.getNode(), url);
	}

	static void wipeKV(Instance instance, String key) {
		String kvtable = "regionkvstore";
		String maintable = "regions";
		String idcolumn = "regionid";
		GPHUD.getDB().d("delete from " + kvtable + " using " + kvtable + "," + maintable + " where " + kvtable + ".k like ? and " + kvtable + "." + idcolumn + "=" + maintable + "." + idcolumn + " and " + maintable + ".instanceid=?", key, instance.getId());
	}

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A Region representation
	 */
	public static Region get(int id) {
		return (Region) factoryPut("Region", id, new Region(id));
	}

	/**
	 * Find a region by name.
	 *
	 * @param name Name of region to locate
	 * @return Region object for that region, or null if none is found.
	 */
	public static Region find(String name) {
		Integer regionid = GPHUD.getDB().dqi(false, "select regionid from regions where name=?", name);
		if (regionid == null) { return null; }
		return get(regionid);
	}

	/**
	 * Register a region against an instance.
	 *
	 * @param region Name of region to register
	 * @param i      Instance object to register the region with
	 * @return A blank string on success, or a text hudMessage explaining any problem.
	 */
	public static String joinInstance(String region, Instance i) {
		// TO DO - lacks validation
		Integer exists = GPHUD.getDB().dqi(true, "select count(*) from regions where name=?", region);
		if (exists == 0) {
			GPHUD.getLogger().info("Joined region '" + region + "' to instance " + i.toString());
			GPHUD.getDB().d("insert into regions(name,instanceid) values(?,?)", region, i.getId());
			return "";
		}
		return "Region is already registered!";
	}

	@Override
	public String getLinkTarget() { return "regions"; }

	/**
	 * Gets the instance associated with this region
	 *
	 * @return The Instance object
	 */
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}

	@Override
	public String getTableName() {
		return "regions";
	}

	@Override
	public String getIdField() {
		return "regionid";
	}

	@Override
	public String getNameField() {
		return "name";
	}

	/**
	 * Gets the URL associated with this region's server
	 *
	 * @return URL
	 */
	public String getURL(boolean permitnull) throws UserException {
		String url = getString("url");
		if (url == null) {
			if (permitnull) { return null; } else { throw new UserException("This region has no callback URL"); }
		}
		return url;
	}

	/**
	 * Gets the URL associated with this region's server
	 *
	 * @return
	 */
	public String getURL() throws UserException {
		return getURL(false);
	}

	/**
	 * Sets the region's URL to callback to the server - CALL THIS FREQUENTLY.
	 * This does not /just/ set the region's url, it checks if it needs to be set firstly, it also updates it if and only if necessary, along with shutting down the URL it replaced.
	 * It also updates the "urllast used" timer if it's more than 60 seconds old.  (used for 'is alive' and 'ping' checking etc etc).
	 *
	 * @param url Targets URL
	 */
	public void setURL(String url) {
		String oldurl = null;
		try { oldurl = getURL(); } catch (UserException e) {} // should only mean there was a null URL
		int now = getUnixTime();

		if (oldurl != null && oldurl.equals(url)) {
			if ((now - getURLLast()) > 60) {
				d("update regions set urllast=?,authnode=? where regionid=?", now, Interface.getNode(), getId());
			}
			return;
		}

		if (oldurl != null && !("".equals(oldurl))) {
			GPHUD.getLogger().info("Sending shutdown to old URL : " + oldurl);
			JSONObject tx = new JSONObject().put("incommand", "shutdown").put("shutdown", "Connection replaced by new region server");
			Transmission t = new Transmission(this, tx, oldurl);
			t.start();
		}

		d("update regions set url=?, urllast=?, authnode=? where regionid=?", url, now, Interface.getNode(), getId());

	}

	/**
	 * Gets the UNIX time the region's server last checked in.
	 *
	 * @return UnixTime the last time this server's url was refreshed / used
	 */
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
	public void verifyAvatars(String[] avatarsarray) {
		String report = "";
		Set<String> avatars = new HashSet<>();
		for (String s : avatarsarray) { avatars.add(s); }
		Results db = dq("select avatarid from visits where regionid=? and endtime is null", getId());
		// iterate over the current visits
		for (ResultsRow row : db) {
			int avatarid = row.getInt("avatarid");
			String name = User.get(avatarid).getName();
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
		for (String s : avatars) {
			GPHUD.getLogger().info("Avatar " + s + " is present on sim but not in visits DB, hopefully they'll register soon.");
		}
	}

	/**
	 * Update the visits noting the following avatars left the sim.
	 *
	 * @param st      State
	 * @param avatars List of Avatar UUIDs or Names that have left the sim.
	 */
	public void departingAvatars(State st, Set<User> avatars) {
		boolean debug = false;
		for (User avatar : avatars) {
			// for all the departing avatars
			if (debug) { System.out.println("Departing " + avatar); }
			try {
				int avatarid = avatar.getId();
				// if the avatar exists, see if there's a visit
				Results rows = dq("select characterid from visits where avatarid=? and regionid=? and endtime is null", avatarid, getId());
				int count = rows.size();
				if (count > 0) {
					st.logger().info("Disconnected avatar " + avatar.getName());
					d("update visits set endtime=? where endtime is null and regionid=? and avatarid=?", UnixTime.getUnixTime(), getId(), avatarid);
				}
				// computer visit XP ((TODO REFACTOR ME?))
				for (ResultsRow r : rows) {
					State temp = new State();
					temp.setInstance(st.getInstance());
					temp.setCharacter(Char.get(r.getInt("characterid")));
					new VisitXP(-1).runAwards(st, temp.getCharacter());
				}
				int instanceid = this.getInstance().getId();
				Results urls = dq("select url from characters where instanceid=? and playedby=? and url is not null", instanceid, avatarid);
				if (debug) { System.out.println("URLs is " + urls.size()); }
				// if the visitor (character) has URLs send them a ping, which will probably 404 and remove its self
				for (ResultsRow row : urls) {
					String url = row.getString("url");
					JSONObject ping = new JSONObject().put("incommand", "ping");
					Transmission t = new Transmission((Char) null, ping, url, 5);
					t.start();
				}
			} catch (Exception e) {
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
	public void recordVersion(State st, String type, String version, String versiondate, String versiontime) {
		Date d = null;
		try {
			SimpleDateFormat df = new SimpleDateFormat("MMM d yyyy HH:mm:ss");
			df.setLenient(true);
			String datetime = versiondate + " " + versiontime;
			datetime = datetime.replaceAll("  ", " ");
			d = df.parse(datetime);
		} catch (ParseException ex) {
			st.logger().log(SEVERE, "Failed to parse date time from " + versiondate + " " + versiontime, ex);
		}
		ResultsRow regiondata = dqone(true, "select region" + type + "version,region" + type + "datetime from regions where regionid=?", getId());
		Integer oldversion = regiondata.getInt("region" + type + "version");
		Integer olddatetime = regiondata.getInt("region" + type + "datetime");
		String[] versionparts = version.split("\\.");
		int newversion = 10000 * Integer.parseInt(versionparts[0]) + 100 * Integer.parseInt(versionparts[1]) + Integer.parseInt(versionparts[2]);
		int newdatetime = (int) (d.getTime() / 1000.0);
		if (oldversion == null || olddatetime == null || olddatetime < newdatetime || oldversion < newversion) {
			d("update regions set region" + type + "version=?,region" + type + "datetime=? where regionid=?", newversion, newdatetime, getId());
			String olddesc = formatVersion(oldversion, olddatetime, false);
			String newdesc = formatVersion(newversion, newdatetime, false);
			st.logger().info("Version upgrade of " + type + " from " + olddesc + " to " + newdesc);
			State fake = new State();
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
	private String formatVersion(Integer version, Integer datetime, boolean html) {
		String v = "";
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
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
	public void recordHUDVersion(State st, String version, String versiondate, String versiontime) {
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
	public void recordServerVersion(State st, String version, String versiondate, String versiontime) {
		recordVersion(st, "server", version, versiondate, versiontime);
	}

	/**
	 * Return the open visits to this region.
	 *
	 * @return Set of Characters currently visiting this region.
	 */
	public Set<Char> getOpenVisits() {
		Set<Char> characters = new TreeSet<>();
		Results results = dq("select characterid from visits where regionid=? and endtime is null", getId());
		for (ResultsRow r : results) {
			characters.add(Char.get(r.getInt("characterid")));
		}
		return characters;
	}
	public Set<User> getAvatarOpenVisits() {
		Set<User> users = new HashSet<>();
		Results results = dq("select avatarid from visits where regionid=? and endtime is null", getId());
		for (ResultsRow r : results) {
			try { users.add(User.get(r.getInt("avatarid"))); }
			catch (Exception e) {}
		}
		return users;
	}

	/**
	 * Return the online/offline status of this region as a string
	 *
	 * @return String, starts with OFFLINE or STALLED if problematic, otherwise "Online"
	 */
	public String getOnlineStatus(String timezone) {
		Integer urllast = getURLLast();
		if (urllast == null) { return "OFFLINE forever?"; }
		if (getURL(true) == null || getURL(true).isEmpty()) {
			return "OFFLINE for " + duration(getUnixTime() - urllast);
		}
		String authnode = getAuthNode();
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
	public String getAuthNode() {
		return dqs(true, "select authnode from regions where regionid=?", getId());
	}

	/**
	 * Extract the currently known server version.
	 *
	 * @param html As HTML?
	 * @return Server version string
	 */
	public String getServerVersion(boolean html) {
		ResultsRow r = dqone(true, "select regionserverversion,regionserverdatetime from regions where regionid=?", getId());
		return formatVersion(r.getInt("regionserverversion"), r.getInt("regionserverdatetime"), html);
	}

	/**
	 * Extract the currently known HUD version.
	 *
	 * @param html As HTML?
	 * @return HUD version string
	 */
	public String getHUDVersion(boolean html) {
		ResultsRow r = dqone(true, "select regionhudversion,regionhuddatetime from regions where regionid=?", getId());
		return formatVersion(r.getInt("regionhudversion"), r.getInt("regionhuddatetime"), html);
	}

	/**
	 * Figure out if this region is running needs to update to the latest version of the server/hud package.
	 *
	 * @return True if the region requires an update, false otherwise
	 */
	public boolean needsUpdate() {
		Integer ourserver = dqi(true, "select regionserverversion from regions where regionid=?", getId());
		Integer maxserver = dqi(false, "select MAX(regionserverversion) from regions");
		if (ourserver != null && maxserver != null) {
			if (maxserver > ourserver) { return true; }
		}
		Integer ourhud = dqi(true, "select regionhudversion from regions where regionid=?", getId());
		Integer maxhud = dqi(false, "select MAX(regionhudversion) from regions");
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
	public void sendServer(JSONObject json) {
		new Transmission(this, json).start();
	}
	public void sendServerSync(JSONObject json) {
		Transmission t=new Transmission(this, json);
		t.run();
		if (t.failed()) { throw new UserException("Connection to server failed"); }
	}

	/**
	 * Get all zones that are present in this region.
	 *
	 * @return Set of Zone objects for this region.
	 */
	public Set<Zone> getZones() {
		Set<Zone> zones = new TreeSet<>();
		for (ResultsRow r : dq("select distinct zoneid from zoneareas where regionid=?", getId())) {
			zones.add(Zone.get(r.getInt()));
		}
		return zones;
	}

	/**
	 * Broadcast the new zoning for this region via the region server
	 */
	public void pushZoning() {
		JSONObject j = new JSONObject();
		j.put("incommand", "broadcast");
		j.put("zoning", ZoneTransport.createZoneTransport(this));
		Transmission t = new Transmission(this, j);
		t.start();
	}

	@Override
	public String getKVTable() {
		return "regionkvstore";
	}

	@Override
	public String getKVIdField() {
		return "regionid";
	}

	public void validate(State st) throws SystemException {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getInstance()) { throw new SystemException("Region / State Instance mismatch"); }
	}

	protected int getNameCacheTime() { return 60 * 60; } // this name doesn't change, cache 1 hour

	public Integer getOpenVisitCount() {
		return dqi(true, "select count(*) from visits where endtime is null and regionid=?", getId());
	}

	public void setGlobalCoordinates(int x, int y) {
		d("update regions set regionx=?,regiony=? where regionid=?",x,y,getId());
	}
    
    /*protected void delete() {
        d("delete from regions where regionid=?",getId());
        Log.log(Log.CRIT, getInstance().getName()+"/"+getName(), "Region", "Deleting region "+getName());
    }*/
}
