package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Experience.Experience;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;
import org.json.JSONObject;

import java.util.*;

import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * Reference to an instance
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Instance extends TableRow {

	private static Map<String, Integer> laststatused = new TreeMap<>(); // naughty, static data, but thats okay really for this

	protected Instance(int id) { super(id); }

	/**
	 * Return instances connected to this node
	 */
	public static Set<Instance> getOurInstances() {
		Set<Instance> instances = new TreeSet<>();
		Results instancerows = GPHUD.getDB().dq("select distinct instances.instanceid from instances,regions where instances.instanceid=regions.instanceid and authnode=?", Interface.getNode());
		for (ResultsRow r : instancerows) { instances.add(Instance.get(r.getInt())); }
		return instances;
	}

	/**
	 * Get all the instances.
	 *
	 * @return Set of Instances
	 */
	public static Set<Instance> getInstances() {
		Set<Instance> instances = new TreeSet<>();
		Results instancerows = GPHUD.getDB().dq("select instanceid from instances");
		for (ResultsRow r : instancerows) { instances.add(Instance.get(r.getInt())); }
		return instances;
	}

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return An Instance representation
	 */
	public static Instance get(Integer id) {
		if (id == null) { return null; }
		return (Instance) factoryPut("Instance", id, new Instance(id));
	}

/*    protected void delete() {
        GPHUD.getLogger().warning(getName()+" DELETING instance "+getName());
        d("delete from instances where instanceid=?",getId());
    }*/

	/**
	 * Create a new instance with a name and owner
	 *
	 * @param name   Name of instance
	 * @param caller Owner of instance
	 * @return Blank string on success, otherwise error hudMessage
	 * @throws UserException If the instance already exists (by name)
	 */
	public static void create(String name, User caller) throws UserException {
		if (name == null || "".equals(name)) { throw new SystemException("Can't create null or empty instance"); }
		if (caller == null) { throw new SystemException("Owner can't be null"); }
		Integer exists = GPHUD.getDB().dqi(false, "select count(*) from instances where name like ?", name);
		if (exists != 0) {
			throw new UserException("Instance already exists!");
		}
		GPHUD.getLogger().info(caller.getName() + " created new instance '" + name + "'");
		GPHUD.getDB().d("insert into instances(owner,name) value(?,?)", caller.getId(), name);
	}

	/**
	 * Find instance by name
	 *
	 * @param name Name of instance
	 * @return Instance object
	 */
	public static Instance find(String name) {
		Integer id = GPHUD.getDB().dqi(false, "select instanceid from instances where name=?", name);
		if (id == null) { throw new UserException("Unable to find instance named '" + name + "'"); }
		return get(id);
	}

	/**
	 * Find instance by owner
	 *
	 * @param owner Avatar to find instances for
	 * @return Set of Instance objects, which may be empty
	 */
	public static Set<Instance> getInstances(User owner) {
		Set<Instance> instances = new TreeSet<>();
		Results results = GPHUD.getDB().dq("select instanceid from instances where owner=?", owner.getId());
		for (ResultsRow r : results) {
			instances.add(get(r.getInt("instanceid")));
		}
		return instances;
	}

	@Override
	public String getLinkTarget() { return "instances"; }

	/**
	 * Get instance owner
	 *
	 * @return Avatar that owns the instance
	 */
	public User getOwner() {
		Integer id = getInt("owner");
		return User.get(id);
	}

	/**
	 * Set the owner of this instance.
	 *
	 * @param id New owner
	 */
	public void setOwner(User id) {
		d("update instances set owner=? where instanceid=?", id.getId(), getId());
	}

	@Override
	public String getTableName() {
		return "instances";
	}

	@Override
	public String getIdField() {
		return "instanceid";
	}

	@Override
	public String getNameField() {
		return "name";
	}

	/**
	 * Create a permissions group in this instance.
	 *
	 * @param name Name of the permissions group
	 * @throws UserException if the group has no name or already exists.
	 */
	public void createPermissionsGroup(String name) throws UserException {
		if (name == null) { throw new UserException("Can not create permissions group with null name"); }
		name = name.trim();
		if (name.isEmpty()) { throw new UserException("Can not create permissions group with blank name"); }
		int exists = dqi(true, "select count(*) from permissionsgroups where name like ? and instanceid=?", name, getId());
		if (exists != 0) { throw new UserException("Group already exists? (" + exists + " results)"); }
		d("insert into permissionsgroups(name,instanceid) values(?,?)", name, getId());
	}

	/**
	 * Get all the permissionsgroups for an instance.
	 *
	 * @return Set of PermissionsGroups
	 */
	public Set<PermissionsGroup> getPermissionsGroups() {
		Results results = dq("select permissionsgroupid from permissionsgroups where instanceid=?", getId());
		Set<PermissionsGroup> set = new TreeSet<>();
		for (ResultsRow r : results) {
			set.add(PermissionsGroup.get(r.getInt("permissionsgroupid")));
		}
		return set;
	}

	//TODO - turn this into a templated call of some kind?

	/**
	 * Get all the regions associated with this instance bound to this server
	 *
	 * @return Set of Regions
	 */
	public Set<Region> getOurRegions(boolean allowretired) {
		Results results = dq("select regionid from regions where instanceid=? and authnode=?", getId(), Interface.getNode());
		Set<Region> regions = new TreeSet<>();
		for (ResultsRow row : results) {
			regions.add(Region.get(row.getInt("regionid"),allowretired));
		}
		return regions;
	}

	/**
	 * Get all the regions associated with this instance
	 *
	 * @return Set of Regions
	 */
	public Set<Region> getRegions(boolean allowretired) {
		Results results = dq("select regionid from regions where instanceid=?", getId());
		Set<Region> regions = new TreeSet<>();
		for (ResultsRow row : results) {
			regions.add(Region.get(row.getInt("regionid"),allowretired));
		}
		return regions;
	}

	/**
	 * Push updated status to all region server.
	 */
	public void updateStatus() {
		String statuscolor = "<0.5,1,0.5>";
		int level = 0;
		String newstatus = "";
		if (GPHUD.DEV) { newstatus += "===DEVELOPMENT===\n \n"; }
		newstatus += "Server: " + GPHUD.hostname + " - " + GPHUD.VERSION + "\n \n";
		//newstatus+=new Date().toString()+" ";
		for (Region r : getRegions(false)) {
			newstatus += "[" + r.getName() + "#";
			Integer visitors = r.getOpenVisitCount();
			String url = dqs(true, "select url from regions where regionid=?", r.getId());
			Integer urllast = dqi(true, "select urllast from regions where regionid=?", r.getId());
			if (urllast == null) { urllast = getUnixTime(); }
			if (url == null || url.isEmpty()) {
				newstatus += "ERROR:DISCONNECTED]";
				if (canStatus("admins")) {
					broadcastAdmins(null, "SYSTEM : Alert, region server for '" + r.getName() + "' is not connected to GPHUD Server.");
				}
				if (level < 2) {
					statuscolor = "<1.0,0.5,0.5>";
					level = 2;
				}
			} else {
				if ((getUnixTime() - urllast) > (15 * 60)) {
					newstatus += "*STALLED*";
					if (canStatus("admins")) {
						broadcastAdmins(null, "SYSTEM : Alert, region server for '" + r.getName() + "' is not communicating (STALLED / CRASHED)??.");
					}
					if (level < 2) {
						statuscolor = "<1.0,0.5,0.5>";
						level = 2;
					}
				}
				newstatus += visitors;
				//System.out.println(r+" - "+r.needsUpdate());
				if (r.needsUpdate()) {
					newstatus += "*UPDATE*";
					if (level < 1) {
						statuscolor = "<1.0,1.0,0.5>";
						level = 1;
					}
				}
				newstatus += "]\n";

			}
		}
		newstatus += " \n";

		JSONObject statusupdate = new JSONObject();
		statusupdate.put("instancestatus", newstatus);
		statusupdate.put("statuscolor", statuscolor);
		for (Region r : getOurRegions(false)) {
			String url = r.getURL(true);
			if (url != null) {
				if (canStatus(url)) {
					Transmission t = new Transmission(r, statusupdate);
					t.start();
				}
			}
		}
	}

	/**
	 * Determine if a message should be sent to the given URL
	 * We rate limit status updates to 1 every 30 seconds for servers.
	 * We use a fake url of "admins" to limit broadcast updates to admins (for server faults).
	 *
	 * @param url The URL of the region server, or "admins" for checking admin updates
	 * @return true if it is permitted to send an update to this target at this time
	 */
	private boolean canStatus(String url) {
		int now = UnixTime.getUnixTime();
		if (laststatused.containsKey(url)) {
			int last = laststatused.get(url);
			if ("admins".equals(url)) {  // bodge, admins get 5 minute pesterings :P
				if ((now - last) > 300) {
					laststatused.put(url, now);
					return true;
				}
				return false;
			}
			if ((now - last) > 30) {
				laststatused.put(url, now);
				return true;
			}
			return false;
		}
		laststatused.put(url, now);
		return true;
	}

	/**
	 * Send a broadcast via the region servers.
	 * This message is encoded as the HUD command "message" causing it to be ownerSayed to all users.
	 *
	 * @param message The message to broadcast to all players at the instance.
	 */
	public void broadcastMessage(String message) {
		GPHUD.getLogger().info("Sending broadcast to instance " + getName() + " - " + message);
		JSONObject j = new JSONObject();
		j.put("message", message);
		j.put("incommand", "broadcast");
		sendServers(j);
	}

	/**
	 * Push a message to all admins of this instance.
	 * Sends message individually to the admins - note this could probably be refitted with the bulk delivery mechanism, on the other hand the number of people getting these alerts is probably small.
	 *
	 * @param st      Session state
	 * @param message Message to send to admins
	 * @return count of the number of users the message is sent to.
	 */
	public int broadcastAdmins(State st, String message) {
		JSONObject j = new JSONObject();
		j.put("message", "ADMIN : " + message);
		Set<User> targets = new HashSet<>();
		targets.add(this.getOwner());
		//System.out.println("Pre broadcast!");
		Results results = dq("select avatarid from permissionsgroupmembers,permissionsgroups,permissions where permissionsgroupmembers.permissionsgroupid=permissionsgroups.permissionsgroupid and permissionsgroups.instanceid=? and permissionsgroups.permissionsgroupid=permissions.permissionsgroupid and permission like 'instance.receiveadminmessages'", getId());
		for (ResultsRow r : results) { targets.add(User.get(r.getInt())); }
		//System.out.println("Avatars:"+targets.size());
		Set<Char> chars = new TreeSet<>();
		for (User a : targets) {
			Results charlist = dq("select characterid from characters where instanceid=? and playedby=? and url is not null", getId(), a.getId());
			for (ResultsRow rr : charlist) { chars.add(Char.get(rr.getInt())); }
		}
		//System.out.println("Characters:"+chars.size());
		for (Char c : chars) {
			Transmission t = new Transmission(c, j);
			t.start();
		}
		if (st != null) {
			st.logger().info("Sent to " + chars.size() + " admins : " + message);
		} else {
			GPHUD.getLogger().info("Sent to " + chars.size() + " admins : " + message);
		}
		return chars.size();
	}

	/**
	 * Returns a list of character summaries for all users in this instance.
	 * This method uses "bulk" database calls to load /all/ users and some summary data about them.
	 * Results are returned as a List of Character objects.
	 * This method has too many connections to "st.uri" and is basically an intrusion by the Web interface into this code, due to the nature of its DB calls.
	 *
	 * @param st Session state, from which sorting will be read via the URI, and instance will be used.
	 * @return A list of CharacterSummary objects
	 * @see CharacterSummary
	 */
	public List<CharacterSummary> getCharacterSummary(State st) {
		String sortby = st.getDebasedURL().replaceAll("%20", " ");
		if (sortby == null) { sortby = ""; }
		sortby = sortby.replaceFirst(".*?sort=", "");
		if (sortby == null) { sortby = ""; }
		boolean reverse = false;
		//System.out.println(sortby+" "+reverse);
		if (sortby.startsWith("-")) {
			reverse = true;
			sortby = sortby.substring(1);
		}
		//System.out.println(sortby+" "+reverse);
		Map<Integer, CharacterSummary> idmap = new TreeMap<>();
		List<String> groupheaders = new ArrayList<>();
		if (st.hasModule("Faction")) {
			groupheaders.add("Faction");
		}
		for (ResultsRow r : dq("select name from attributes where instanceid=? and grouptype is not null and attributetype='GROUP'", getId())) {
			groupheaders.add(r.getString());
		}
		for (ResultsRow r : dq("select * from characters where instanceid=?", getId())) {
			Integer retired = r.getInt("retired");
			int charid = r.getInt("characterid");
			CharacterSummary cr = new CharacterSummary();
			cr.id = charid;
			cr.lastactive = r.getInt("lastactive");
			cr.name = r.getString("name");
			cr.ownerid = r.getInt("owner");
			cr.groupheaders = groupheaders;
			if (retired != 1) { cr.retired = false; } else { cr.retired = true; }
			cr.online = false;
			if (r.getString("url") != null && !r.getString("url").isEmpty()) { cr.online = true; }
			idmap.put(charid, cr);
		}
		for (ResultsRow r : dq("select charactergroupmembers.characterid,charactergroups.type,charactergroups.name from charactergroupmembers,charactergroups where charactergroupmembers.charactergroupid=charactergroups.charactergroupid and instanceid=?", getId())) {
			int charid = r.getInt("characterid");
			String grouptype = r.getString("type");
			String groupname = r.getString("name");
			if (idmap.containsKey(charid)) {
				CharacterSummary cr = idmap.get(charid);
				cr.setGroup(grouptype, groupname);
			}
		}
		for (ResultsRow r : dq("select characterid,sum(endtime-starttime) as totaltime from visits where endtime is not null group by characterid")) {
			int id = r.getInt("characterid");
			if (idmap.containsKey(id)) { idmap.get(id).totalvisits = r.getInt("totaltime"); }
		}
		for (ResultsRow r : dq("select characterid,sum(endtime-starttime) as totaltime from visits where endtime is not null and starttime>? group by characterid", UnixTime.getUnixTime() - (Experience.getCycle(st)))) {
			int id = r.getInt("characterid");
			if (idmap.containsKey(id)) { idmap.get(id).recentvisits = r.getInt("totaltime"); }
		}
		for (ResultsRow r : dq("select characterid,starttime from visits where endtime is null and starttime>?", UnixTime.getUnixTime() - (Experience.getCycle(st)))) {
			int id = r.getInt("characterid");
			int add = UnixTime.getUnixTime() - r.getInt("starttime");
			if (idmap.containsKey(id)) {
				idmap.get(id).recentvisits = idmap.get(id).recentvisits + add;
				idmap.get(id).totalvisits = idmap.get(id).totalvisits + add;
			}
		}
		for (ResultsRow r : dq("select characterid,sum(adjustment) as total from characterpools where poolname like 'Experience.%' or poolname like 'Faction.FactionXP' group by characterid")) {
			int id = r.getInt("characterid");
			if (idmap.containsKey(id)) { idmap.get(id).totalxp = r.getInt("total"); }
		}
		Map<Integer, String> avatarnames = new TreeMap<>();
		for (ResultsRow r : SL.getDB().dq("select id,username from users")) {
			avatarnames.put(r.getInt("id"), r.getString("username"));
		}
		for (CharacterSummary cs : idmap.values()) {
			cs.ownername = avatarnames.get(cs.ownerid);
		}

		List<CharacterSummary> sortedlist = new ArrayList<>();
		if (sortby == null || sortby.isEmpty()) { sortby = "name"; }
		sortby = sortby.toLowerCase();
		if ("name".equals(sortby) || "owner".equals(sortby)) {
			Map<String, Set<CharacterSummary>> sorted = new TreeMap<>();
			for (CharacterSummary cs : idmap.values()) {
				String value = cs.name;
				if ("owner".equals(sortby)) { value = cs.ownername; }
				Set<CharacterSummary> records = new HashSet<>();
				if (sorted.containsKey(value)) { records = sorted.get(value); }
				records.add(cs);
				sorted.put(value, records);
			}
			List<String> sortedkeys = new ArrayList<>();
			sortedkeys.addAll(sorted.keySet());
			if (reverse) { Collections.sort(sortedkeys, Collections.reverseOrder()); } else {
				Collections.sort(sortedkeys);
			}
			for (String key : sortedkeys) {
				Set<CharacterSummary> set = sorted.get(key);
				for (CharacterSummary c : set) { sortedlist.add(c); }
			}
		} else {
			Map<Integer, Set<CharacterSummary>> sorted = new TreeMap<>();
			for (CharacterSummary cs : idmap.values()) {
				Integer value = cs.lastactive;
				if ("total visit time".equals(sortby)) { value = cs.totalvisits; }
				if (sortby.equals("visit time (last " + Experience.getCycleLabel(st).toLowerCase() + ")")) {
					value = cs.recentvisits;
				}
				if ("total xp".equals(sortby)) { value = cs.totalxp; }
				if ("level".equals(sortby)) { value = cs.totalxp; }

				Set<CharacterSummary> records = new TreeSet<>();
				if (sorted.containsKey(value)) { records = sorted.get(value); }
				records.add(cs);
				sorted.put(value, records);
			}
			List<Integer> sortedkeys = new ArrayList<>();
			sortedkeys.addAll(sorted.keySet());
			if (!reverse) { Collections.sort(sortedkeys, Collections.reverseOrder()); } else {
				Collections.sort(sortedkeys);
			} // note reverse is reversed for numbers
			// default is biggest at top, smallest at bottom, which is reverse order as the NORMAL order.   alphabetic is a-z so forward order for the NORMAL order....
			for (Integer key : sortedkeys) {
				Set<CharacterSummary> set = sorted.get(key);
				for (CharacterSummary c : set) { sortedlist.add(c); }
			}

		}


		return sortedlist;
	}

	/**
	 * Get the character groups at this instance by type.
	 *
	 * @param keyword Type of group to find
	 * @return Set of character groups (potentially the empty set)
	 */
	public Set<CharacterGroup> getGroupsForKeyword(String keyword) {
		Set<CharacterGroup> groups = new TreeSet<>();
		for (ResultsRow r : dq("select charactergroupid from charactergroups where instanceid=? and type=?", getId(), keyword)) {
			groups.add(CharacterGroup.get(r.getInt()));
		}
		return groups;
	}

	/**
	 * Create a character group.
	 *
	 * @param name    Name of the group to create
	 * @param open    Is the group open to join (otherwise invite only)?
	 * @param keyword Type of the group, optionally.
	 * @throws UserException If the group can not be created, already exists, etc.
	 */
	public void createCharacterGroup(String name, boolean open, String keyword) {
		int count = dqi(true, "select count(*) from charactergroups where instanceid=? and name like ?", getId(), name);
		if (count > 0) { throw new UserException("Failed to create group, already exists."); }
		d("insert into charactergroups(instanceid,name,open,type) values (?,?,?,?)", getId(), name, open, keyword);
	}

	/**
	 * Get all character groups for this instance.
	 * Ignores the group type element.
	 *
	 * @return Set of CharacterGroups
	 */
	public Set<CharacterGroup> getCharacterGroups() {
		Set<CharacterGroup> groups = new TreeSet<>();
		for (ResultsRow r : dq("select charactergroupid from charactergroups where instanceid=?", getId())) {
			groups.add(CharacterGroup.get(r.getInt()));
		}
		return groups;
	}

	/**
	 * Transmit a JSON message to all regions servers for this instance.
	 *
	 * @param j JSON message to transmit.
	 */
	public void sendServers(JSONObject j) {
		for (Region r : getRegions(false)) {
			r.sendServer(j);
			//System.out.println("Send to "+r.getName()+" "+j.toString());
		}
	}

	/**
	 * Get a zone by name
	 *
	 * @param name Name of zone
	 * @return Zone object, or null.
	 */
	public Zone getZone(String name) {
		Integer id = dqi(false, "select zoneid from zones where instanceid=? and name like ?", getId(), name);
		if (id == null) { return null; }
		return Zone.get(id);
	}

	/**
	 * Get a list of all zones.
	 *
	 * @return Set (possibly empty) of Zones
	 */
	public Set<Zone> getZones() {
		Set<Zone> zones = new TreeSet<>();
		for (ResultsRow r : dq("select zoneid from zones where instanceid=?", getId())) {
			zones.add(Zone.get(r.getInt()));
		}
		return zones;
	}

	@Override
	public String getKVTable() {
		return "instancekvstore";
	}

	@Override
	public String getKVIdField() {
		return "instanceid";
	}

	/**
	 * Get all events for this instance
	 *
	 * @return Set of Events
	 */
	public Set<Event> getEvents() {
		return Event.getAll(this);
	}

	/**
	 * Get all currently active events for this instance
	 *
	 * @return Set of events that are active and have been started for this instance
	 */
	public Set<Event> getActiveEvents() {
		return Event.getActive(this);
	}

	/**
	 * Get all currently active event schedules.
	 *
	 * @return Set of EventSchedules that are currently active and have been started
	 */
	public Set<EventSchedule> getActiveEventSchedules() {
		return EventSchedule.getActive(this);
	}

	/**
	 * Recompute all possibly conveyances for all logged in characters.
	 * Bulk update via server dissemination where necessary.
	 */
	public void pushConveyances() {
		boolean debug = false;
		Map<Region, JSONObject> buffer = new TreeMap<>();
		for (Char c : Char.getActive(this)) {
			State simulated = new State(c);
			JSONObject payload = new JSONObject();
			simulated.getCharacter().appendConveyance(simulated, payload);
			if (debug) {
				System.out.println("PUSH CONSIDERATION : " + simulated.getCharacter() + " - " + payload.toString());
			}
			if (!payload.keySet().isEmpty()) {
				Region reg = c.getRegion();
				if (reg != null) {
					if (debug) { System.out.println("Adding"); }
					if (!buffer.containsKey(reg)) {
						if (debug) { System.out.println("created buffer"); }
						buffer.put(reg, new JSONObject().put("incommand", "disseminate"));
					}
					User user = c.getPlayedBy();
					if (user!=null) {
						String playedby = user.getUUID();
						String payloadstring = payload.toString();
						buffer.get(reg).put(playedby, payloadstring);
						if (buffer.get(reg).toString().length() > (3 * 1024)) {
							if (debug) {
								GPHUD.getLogger("Instance " + getNameSafe()).severe("Size cautionary push: " + reg + " : " + buffer.get(reg).toString());
							}
							reg.sendServer(buffer.get(reg));
							buffer.put(reg, new JSONObject().put("incommand", "disseminate"));
						}
					}
				}
			}
		}
		for (Region region : buffer.keySet()) {
			if (debug) { System.out.println("PUSHREGION " + region + " : " + buffer.get(region)); }
			region.sendServer(buffer.get(region));
		}
	}

	public void validate(State st) throws SystemException {
		if (validated) { return; }
		validate();
	}

	protected int getNameCacheTime() { return 60 * 60; } // this name doesn't change, cache 1 hour

	public void createAttribute(String name, Boolean selfmodify, String attributetype, String grouptype, Boolean usesabilitypoints, Boolean required, String defaultvalue) {
		Attribute.create(this, name, selfmodify, attributetype, grouptype, usesabilitypoints, required, defaultvalue);
	}

	void wipeKV(String key) {
		CharacterGroup.wipeKV(this, key);
		Event.wipeKV(this, key);
		Char.wipeKV(this, key);
		Zone.wipeKV(this, key);
		Region.wipeKV(this, key);
		d("delete from instancekvstore where instanceid=? and k like ?", getId(), key);
	}

	public String getLogoURL(State st) {
		String logouuid = st.getKV(this, "GPHUDClient.logo");
		if (logouuid == null || logouuid.isEmpty()) { return "/resources/banner-gphud.png"; }
		return "http://texture-service.agni.lindenlab.com/" + logouuid + "/320x240.jpg/";
	}

	public String getLogoHREF(State st) {
		return "<a href=\"" + getLogoURL(st) + "\">";
	}

	public Set<Scripts> getScripts() {
		return Scripts.getScript(this);
	}
}
