package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputEmptyException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Experience.Experience;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * Reference to an instance
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Instance extends TableRow {

	private static final Map<String,Integer> laststatused=new TreeMap<>(); // naughty, static data, but thats okay really for this

	protected Instance(final int id) { super(id); }

	// ---------- STATICS ----------

	/**
	 * Return instances connected to this node
	 */
	@Nonnull
	public static Set<Instance> getOurInstances() {
		final Set<Instance> instances=new TreeSet<>();
		final Results instancerows=GPHUD.getDB()
		                                .dq("select distinct instances.instanceid from instances,regions where instances.instanceid=regions.instanceid and authnode=? and "+"retired=0",
		                                    Interface.getNode()
		                                   );
		for (final ResultsRow r: instancerows) { instances.add(Instance.get(r.getInt())); }
		return instances;
	}

	/**
	 * Get all the instances.
	 *
	 * @return Set of Instances
	 */
	@Nonnull
	public static Set<Instance> getInstances() {
		final Set<Instance> instances=new TreeSet<>();
		final Results instancerows=GPHUD.getDB().dq("select instanceid from instances");
		for (final ResultsRow r: instancerows) { instances.add(Instance.get(r.getInt())); }
		return instances;
	}

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 *
	 * @return An Instance representation
	 */
	@Nonnull
	public static Instance get(@Nonnull final Integer id) {
		return (Instance) factoryPut("Instance",id,new Instance(id));
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
	 *
	 * @return Blank string on success, otherwise error hudMessage
	 *
	 * @throws UserException If the instance already exists (by name)
	 */
	public static void create(@Nullable final String name,
	                          @Nullable final User caller) {
		if (name==null || "".equals(name)) { throw new SystemBadValueException("Can't create null or empty instance"); }
		if (caller==null) { throw new SystemBadValueException("Owner can't be null"); }
		final int exists=GPHUD.getDB().dqinn("select count(*) from instances where name like ?",name);
		if (exists!=0) {
			throw new UserInputDuplicateValueException("Instance already exists!");
		}
		GPHUD.getLogger().info(caller.getName()+" created new instance '"+name+"'");
		GPHUD.getDB().d("insert into instances(owner,name) value(?,?)",caller.getId(),name);
	}

	/**
	 * Find instance by name
	 *
	 * @param name Name of instance
	 *
	 * @return Instance object
	 */
	@Nonnull
	public static Instance find(final String name) {
		try {
			final int id=GPHUD.getDB().dqinn("select instanceid from instances where name=?",name);
			return get(id);
		}
		catch (@Nonnull final NoDataException e) {
			throw new UserInputLookupFailureException("Unable to find instance named '"+name+"'",e);
		}
	}

	/**
	 * Find instance by owner
	 *
	 * @param owner Avatar to find instances for
	 *
	 * @return Set of Instance objects, which may be empty
	 */
	@Nonnull
	public static Set<Instance> getInstances(@Nonnull final User owner) {
		final Set<Instance> instances=new TreeSet<>();
		final Results results=GPHUD.getDB().dq("select instanceid from instances where owner=?",owner.getId());
		for (final ResultsRow r: results) {
			instances.add(get(r.getInt("instanceid")));
		}
		return instances;
	}

	// ---------- INSTANCE ----------

	/**
	 * Get instance owner
	 *
	 * @return Avatar that owns the instance
	 */
	public User getOwner() {
		return User.get(getInt("owner"));
	}

	/**
	 * Set the owner of this instance.
	 *
	 * @param id New owner
	 */
	public void setOwner(@Nonnull final User id) {
		d("update instances set owner=? where instanceid=?",id.getId(),getId());
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "instances";
	}

	@Nonnull
	@Override
	public String getIdColumn() {
		return "instanceid";
	}

	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}

	@Nonnull
	@Override
	public String getLinkTarget() { return "instances"; }

	@Nonnull
	@Override
	public String getKVTable() {
		return "instancekvstore";
	}

	//TODO - turn this into a templated call of some kind?

	@Nonnull
	@Override
	public String getKVIdField() {
		return "instanceid";
	}

	protected int getNameCacheTime() { return 60*60; } // this name doesn't change, cache 1 hour

	/**
	 * Create a permissions group in this instance.
	 *
	 * @param name Name of the permissions group
	 *
	 * @throws UserException if the group has no name or already exists.
	 */
	public void createPermissionsGroup(@Nullable String name) {
		if (name==null) { throw new UserInputEmptyException("Can not create permissions group with null name"); }
		name=name.trim();
		if (name.isEmpty()) { throw new UserInputEmptyException("Can not create permissions group with blank name"); }
		final int exists=dqinn("select count(*) from permissionsgroups where name like ? and instanceid=?",name,getId());
		if (exists!=0) { throw new UserInputDuplicateValueException("Group already exists? ("+exists+" results)"); }
		d("insert into permissionsgroups(name,instanceid) values(?,?)",name,getId());
	}

	/**
	 * Get all the permissionsgroups for an instance.
	 *
	 * @return Set of PermissionsGroups
	 */
	@Nonnull
	public Set<PermissionsGroup> getPermissionsGroups() {
		final Results results=dq("select permissionsgroupid from permissionsgroups where instanceid=?",getId());
		final Set<PermissionsGroup> set=new TreeSet<>();
		for (final ResultsRow r: results) {
			set.add(PermissionsGroup.get(r.getInt("permissionsgroupid")));
		}
		return set;
	}

	/**
	 * Get all the regions associated with this instance bound to this server
	 *
	 * @return Set of Regions
	 */
	@Nonnull
	public Set<Region> getOurRegions(final boolean allowretired) {
		final Results results=dq("select regionid from regions where instanceid=? and authnode=? and retired<?",getId(),Interface.getNode(),allowretired?2:1);
		final Set<Region> regions=new TreeSet<>();
		for (final ResultsRow row: results) {
			regions.add(Region.get(row.getInt("regionid"),allowretired));
		}
		return regions;
	}

	/**
	 * Get all the regions associated with this instance
	 *
	 * @return Set of Regions
	 */
	@Nonnull
	public Set<Region> getRegions(final boolean allowretired) {
		final Results results=dq("select regionid from regions where instanceid=? and retired<?",getId(),allowretired?2:1);
		final Set<Region> regions=new TreeSet<>();
		for (final ResultsRow row: results) {
			regions.add(Region.get(row.getInt("regionid"),allowretired));
		}
		return regions;
	}

	/**
	 * Push updated status to all region server.
	 */
	public void updateStatus() {
		String statuscolor="<0.5,1,0.5>";
		int level=0;
		final StringBuilder newstatus=new StringBuilder();
		if (GPHUD.DEV) { newstatus.append("===DEVELOPMENT===\n \n"); }
		newstatus.append("Server: ").append(GPHUD.hostname).append(" - ").append(GPHUD.VERSION).append("\n \n");
		//newstatus+=new Date().toString()+" ";
		for (final Region r: getRegions(false)) {
			newstatus.append("[").append(r.getName()).append("#");
			final Integer visitors=r.getOpenVisitCount();
			final String url=dqs("select url from regions where regionid=?",r.getId());
			Integer urllast=dqi("select urllast from regions where regionid=?",r.getId());
			if (urllast==null) { urllast=getUnixTime(); }
			if (url==null || url.isEmpty()) {
				newstatus.append("ERROR:DISCONNECTED]");
				if (canStatus("admins")) {
					broadcastAdmins(null,"SYSTEM : Alert, region server for '"+r.getName()+"' is not connected to GPHUD Server.");
				}
				if (level<2) {
					statuscolor="<1.0,0.5,0.5>";
					level=2;
				}
			}
			else {
				if ((getUnixTime()-urllast)>(15*60)) {
					newstatus.append("*STALLED*");
					if (canStatus("admins")) {
						broadcastAdmins(null,"SYSTEM : Alert, region server for '"+r.getName()+"' is not communicating (STALLED / CRASHED)??.");
					}
					if (level<2) {
						statuscolor="<1.0,0.5,0.5>";
						level=2;
					}
				}
				newstatus.append(visitors);
				//System.out.println(r+" - "+r.needsUpdate());
				if (r.needsUpdate()) {
					newstatus.append("*UPDATE*");
					if (level<1) {
						statuscolor="<1.0,1.0,0.5>";
						level=1;
					}
				}
				newstatus.append("]\n");

			}
		}
		newstatus.append(" \n");

		final JSONObject statusupdate=new JSONObject();
		statusupdate.put("instancestatus",newstatus.toString());
		statusupdate.put("statuscolor",statuscolor);
		for (final Region r: getOurRegions(false)) {
			final String url=r.getURLNullable();
			if (url!=null) {
				if (canStatus(url)) {
					final Transmission t=new Transmission(r,statusupdate);
					t.start();
				}
			}
		}
	}

	/**
	 * Send a broadcast via the region servers.
	 * This message is encoded as the HUD command "message" causing it to be ownerSayed to all users.
	 *
	 * @param message The message to broadcast to all players at the instance.
	 */
	public void broadcastMessage(final String message) {
		GPHUD.getLogger().info("Sending broadcast to instance "+getName()+" - "+message);
		final JSONObject j=new JSONObject();
		j.put("message",message);
		j.put("incommand","broadcast");
		sendServers(j);
	}

	/**
	 * Push a message to all admins of this instance.
	 * Sends message individually to the admins - note this could probably be refitted with the bulk delivery mechanism, on the other hand the number of people getting these
	 * alerts is probably small.
	 *
	 * @param st      Session state
	 * @param message Message to send to admins
	 *
	 * @return count of the number of users the message is sent to.
	 */
	public int broadcastAdmins(@Nullable final State st,
	                           final String message) {
		final JSONObject j=new JSONObject();
		j.put("message","ADMIN : "+message);
		final Set<User> targets=new HashSet<>();
		targets.add(getOwner());
		//System.out.println("Pre broadcast!");
		final Results results=dq(
				"select avatarid from permissionsgroupmembers,permissionsgroups,permissions where permissionsgroupmembers.permissionsgroupid=permissionsgroups"+".permissionsgroupid and permissionsgroups.instanceid=? and permissionsgroups.permissionsgroupid=permissions.permissionsgroupid and permission like "+"'instance.receiveadminmessages'",
				getId()
		                        );
		for (final ResultsRow r: results) { targets.add(User.get(r.getInt())); }
		//System.out.println("Avatars:"+targets.size());
		final Set<Char> chars=new TreeSet<>();
		for (final User a: targets) {
			final Results charlist=dq("select characterid from characters where instanceid=? and playedby=? and url is not null",getId(),a.getId());
			for (final ResultsRow rr: charlist) { chars.add(Char.get(rr.getInt())); }
		}
		//System.out.println("Characters:"+chars.size());
		for (final Char c: chars) {
			final Transmission t=new Transmission(c,j);
			t.start();
		}
		if (st!=null) {
			st.logger().info("Sent to "+chars.size()+" admins : "+message);
		}
		else {
			GPHUD.getLogger().info("Sent to "+chars.size()+" admins : "+message);
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
	 *
	 * @return A list of CharacterSummary objects
	 */
	@Nonnull
	public List<CharacterSummary> getCharacterSummary(@Nonnull final State st) {
		String sortby=st.getDebasedURL().replaceAll("%20"," ");
		sortby=sortby.replaceFirst(".*?sort=","");
		boolean reverse=false;
		//System.out.println(sortby+" "+reverse);
		if (sortby.startsWith("-")) {
			reverse=true;
			sortby=sortby.substring(1);
		}
		//System.out.println(sortby+" "+reverse);
		final Map<Integer,CharacterSummary> idmap=new TreeMap<>();
		final List<String> groupheaders=new ArrayList<>();
		if (st.hasModule("Faction")) {
			groupheaders.add("Faction");
		}
		for (final ResultsRow r: dq("select name from attributes where instanceid=? and grouptype is not null and attributetype='GROUP'",getId())) {
			groupheaders.add(r.getStringNullable());
		}
		for (final ResultsRow r: dq("select * from characters where instanceid=?",getId())) {
			final int retired=r.getInt("retired");
			final int charid=r.getInt("characterid");
			final CharacterSummary cr=new CharacterSummary();
			cr.id=charid;
			cr.lastactive=r.getInt("lastactive");
			cr.name=r.getString("name");
			cr.ownerid=r.getInt("owner");
			cr.groupheaders=groupheaders;
			if (retired!=1) { cr.retired=false; }
			else { cr.retired=true; }
			cr.online=false;
			if (r.getStringNullable("url")!=null && !r.getString("url").isEmpty()) { cr.online=true; }
			idmap.put(charid,cr);
		}
		for (final ResultsRow r: dq(
				"select charactergroupmembers.characterid,charactergroups.type,charactergroups.name from charactergroupmembers,charactergroups where charactergroupmembers"+".charactergroupid=charactergroups.charactergroupid and instanceid=?",
				getId()
		                           )) {
			final int charid=r.getInt("characterid");
			final String grouptype=r.getStringNullable("type");
			final String groupname=r.getStringNullable("name");
			if (idmap.containsKey(charid)) {
				final CharacterSummary cr=idmap.get(charid);
				cr.setGroup(grouptype,groupname);
			}
		}
		for (final ResultsRow r: dq("select characterid,sum(endtime-starttime) as totaltime from visits where endtime is not null group by characterid")) {
			final int id=r.getInt("characterid");
			if (idmap.containsKey(id)) { idmap.get(id).totalvisits=r.getInt("totaltime"); }
		}
		for (final ResultsRow r: dq("select characterid,sum(endtime-starttime) as totaltime from visits where endtime is not null and starttime>? group by characterid",
		                            UnixTime.getUnixTime()-(Experience.getCycle(st))
		                           )) {
			final int id=r.getInt("characterid");
			if (idmap.containsKey(id)) { idmap.get(id).recentvisits=r.getInt("totaltime"); }
		}
		for (final ResultsRow r: dq("select characterid,starttime from visits where endtime is null and starttime>?",UnixTime.getUnixTime()-(Experience.getCycle(st)))) {
			final int id=r.getInt("characterid");
			final int add=UnixTime.getUnixTime()-r.getInt("starttime");
			if (idmap.containsKey(id)) {
				idmap.get(id).recentvisits=idmap.get(id).recentvisits+add;
				idmap.get(id).totalvisits=idmap.get(id).totalvisits+add;
			}
		}
		for (final ResultsRow r: dq(
				"select characterid,sum(adjustment) as total from characterpools where poolname like 'Experience.%' or poolname like 'Faction.FactionXP' group by "+"characterid")) {
			final int id=r.getInt("characterid");
			if (idmap.containsKey(id)) { idmap.get(id).totalxp=r.getInt("total"); }
		}
		final Map<Integer,String> avatarnames=new TreeMap<>();
		for (final ResultsRow r: SL.getDB().dq("select id,username from users")) {
			avatarnames.put(r.getInt("id"),r.getString("username"));
		}
		for (final CharacterSummary cs: idmap.values()) {
			cs.ownername=avatarnames.get(cs.ownerid);
		}

		final List<CharacterSummary> sortedlist=new ArrayList<>();
		if (sortby.isEmpty()) { sortby="name"; }
		sortby=sortby.toLowerCase();
		if ("name".equals(sortby) || "owner".equals(sortby)) {
			final Map<String,Set<CharacterSummary>> sorted=new TreeMap<>();
			for (final CharacterSummary cs: idmap.values()) {
				String value=cs.name;
				if ("owner".equals(sortby)) { value=cs.ownername; }
				Set<CharacterSummary> records=new HashSet<>();
				if (sorted.containsKey(value)) { records=sorted.get(value); }
				records.add(cs);
				sorted.put(value,records);
			}
			final List<String> sortedkeys=new ArrayList<>(sorted.keySet());
			if (reverse) { sortedkeys.sort(Collections.reverseOrder()); }
			else {
				Collections.sort(sortedkeys);
			}
			for (final String key: sortedkeys) {
				final Set<CharacterSummary> set=sorted.get(key);
				sortedlist.addAll(set);
			}
		}
		else {
			final Map<Integer,Set<CharacterSummary>> sorted=new TreeMap<>();
			for (final CharacterSummary cs: idmap.values()) {
				int value=cs.lastactive;
				if ("total visit time".equals(sortby)) { value=cs.totalvisits; }
				if (sortby.equals("visit time (last "+Experience.getCycleLabel(st).toLowerCase()+")")) {
					value=cs.recentvisits;
				}
				if ("total xp".equals(sortby)) { value=cs.totalxp; }
				if ("level".equals(sortby)) { value=cs.totalxp; }

				Set<CharacterSummary> records=new TreeSet<>();
				if (sorted.containsKey(value)) { records=sorted.get(value); }
				records.add(cs);
				sorted.put(value,records);
			}
			final List<Integer> sortedkeys=new ArrayList<>(sorted.keySet());
			if (!reverse) { sortedkeys.sort(Collections.reverseOrder()); }
			else {
				Collections.sort(sortedkeys);
			} // note reverse is reversed for numbers
			// default is biggest at top, smallest at bottom, which is reverse order as the NORMAL order.   alphabetic is a-z so forward order for the NORMAL order....
			for (final Integer key: sortedkeys) {
				final Set<CharacterSummary> set=sorted.get(key);
				sortedlist.addAll(set);
			}

		}


		return sortedlist;
	}

	/**
	 * Get the character groups at this instance by type.
	 *
	 * @param keyword Type of group to find
	 *
	 * @return Set of character groups (potentially the empty set)
	 */
	@Nonnull
	public Set<CharacterGroup> getGroupsForKeyword(final String keyword) {
		final Set<CharacterGroup> groups=new TreeSet<>();
		for (final ResultsRow r: dq("select charactergroupid from charactergroups where instanceid=? and type=?",getId(),keyword)) {
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
	 *
	 * @throws UserException If the group can not be created, already exists, etc.
	 */
	public void createCharacterGroup(final String name,
	                                 final boolean open,
	                                 final String keyword) {
		final int count=dqinn("select count(*) from charactergroups where instanceid=? and name like ?",getId(),name);
		if (count>0) { throw new UserInputDuplicateValueException("Failed to create group, already exists."); }
		d("insert into charactergroups(instanceid,name,open,type) values (?,?,?,?)",getId(),name,open,keyword);
	}

	/**
	 * Get all character groups for this instance.
	 * Ignores the group type element.
	 *
	 * @return Set of CharacterGroups
	 */
	@Nonnull
	public Set<CharacterGroup> getCharacterGroups() {
		final Set<CharacterGroup> groups=new TreeSet<>();
		for (final ResultsRow r: dq("select charactergroupid from charactergroups where instanceid=?",getId())) {
			groups.add(CharacterGroup.get(r.getInt()));
		}
		return groups;
	}

	/**
	 * Transmit a JSON message to all regions servers for this instance.
	 *
	 * @param j JSON message to transmit.
	 */
	public void sendServers(final JSONObject j) {
		for (final Region r: getRegions(false)) {
			r.sendServer(j);
			//System.out.println("Send to "+r.getName()+" "+j.toString());
		}
	}

	/**
	 * Get a zone by name
	 *
	 * @param name Name of zone
	 *
	 * @return Zone object, or null.
	 */
	@Nullable
	public Zone getZone(final String name) {
		try {
			final int id=dqinn("select zoneid from zones where instanceid=? and name like ?",getId(),name);
			return Zone.get(id);
		}
		catch (@Nonnull final NoDataException e) { return null; }
	}

	/**
	 * Get a list of all zones.
	 *
	 * @return Set (possibly empty) of Zones
	 */
	@Nonnull
	public Set<Zone> getZones() {
		final Set<Zone> zones=new TreeSet<>();
		for (final ResultsRow r: dq("select zoneid from zones where instanceid=?",getId())) {
			zones.add(Zone.get(r.getInt()));
		}
		return zones;
	}

	/**
	 * Get all events for this instance
	 *
	 * @return Set of Events
	 */
	@Nonnull
	public Set<Event> getEvents() {
		return Event.getAll(this);
	}

	/**
	 * Get all currently active events for this instance
	 *
	 * @return Set of events that are active and have been started for this instance
	 */
	@Nonnull
	public Set<Event> getActiveEvents() {
		return Event.getActive(this);
	}

	/**
	 * Get all currently active event schedules.
	 *
	 * @return Set of EventSchedules that are currently active and have been started
	 */
	@Nonnull
	public Set<EventSchedule> getActiveEventSchedules() {
		return EventSchedule.getActive(this);
	}

	/**
	 * Recompute all possibly conveyances for all logged in characters.
	 * Bulk update via server dissemination where necessary.
	 */
	public void pushConveyances() {
		final boolean debug=false;
		final Map<Region,JSONObject> buffer=new TreeMap<>();
		for (final Char c: Char.getActive(this)) {
			final State simulated=new State(c);
			final JSONObject payload=new JSONObject();
			simulated.getCharacter().appendConveyance(simulated,payload);
			if (!payload.keySet().isEmpty()) {
				final Region reg=c.getRegion();
				if (reg!=null) {
					if (!buffer.containsKey(reg)) {
						buffer.put(reg,new JSONObject().put("incommand","disseminate"));
					}
					final User user=c.getPlayedBy();
					String playedby=null;
					if (user!=null) { playedby=user.getUUID(); }
					if (playedby==null) { // maybe its an object
						try {
							playedby=dqs("select uuid from objects where url like ?",c.getURL());
						}
						catch (@Nonnull final NoDataException e) {}
					}
					if (playedby!=null) {
						final String payloadstring=payload.toString();
						buffer.get(reg).put(playedby,payloadstring);
						if (buffer.get(reg).toString().length()>(1024+512+256+128+64)) {
							reg.sendServer(buffer.get(reg));
							buffer.put(reg,new JSONObject().put("incommand","disseminate"));
						}
					}
				}
			}
		}
		for (final Map.Entry<Region,JSONObject> entry: buffer.entrySet()) {
			final Region region=entry.getKey();
			region.sendServer(entry.getValue());
		}
	}

	public void createAttribute(final String name,
	                            final Boolean selfmodify,
	                            final Attribute.ATTRIBUTETYPE attributetype,
	                            final String grouptype,
	                            final Boolean usesabilitypoints,
	                            final Boolean required,
	                            final String defaultvalue) {
		Attribute.create(this,name,selfmodify,attributetype,grouptype,usesabilitypoints,required,defaultvalue);
	}

	@Nonnull
	public String getLogoURL(@Nonnull final State st) {
		final String logouuid=st.getKV(this,"GPHUDClient.logo");
		if (logouuid==null || logouuid.isEmpty()) { return "/resources/banner-gphud.png"; }
		return SL.textureURL(logouuid);
	}

	public int getLogoWidth(@Nonnull final State st,
	                        final float height) {
		final State fakestate=new State(this);
		final float multiplier=fakestate.getKV("GPHUDClient.widthmultiplier").floatValue();
		return (int) (height*multiplier);
	}

	@Nonnull
	public String getLogoHREF(@Nonnull final State st) {
		return "<a href=\""+getLogoURL(st)+"\">";
	}

	@Nonnull
	public Set<Scripts> getScripts() {
		return Scripts.getScripts(this);
	}

	@Nullable
	public Landmarks getLandmark(final String name) { return Landmarks.find(this,name); }

	@Nonnull
	public Set<Landmarks> getLandmarks() {
		return Landmarks.getAll(this);
	}

	// ----- Internal Instance -----
	void wipeKV(final String key) {
		CharacterGroup.wipeKV(this,key);
		Event.wipeKV(this,key);
		Char.wipeKV(this,key);
		Zone.wipeKV(this,key);
		Region.wipeKV(this,key);
		Effect.wipeKV(this,key);
		d("delete from instancekvstore where instanceid=? and k like ?",getId(),key);
	}

	/**
	 * Determine if a message should be sent to the given URL
	 * We rate limit status updates to 1 every 30 seconds for servers.
	 * We use a fake url of "admins" to limit broadcast updates to admins (for server faults).
	 *
	 * @param url The URL of the region server, or "admins" for checking admin updates
	 *
	 * @return true if it is permitted to send an update to this target at this time
	 */
	private boolean canStatus(final String url) {
		final int now=UnixTime.getUnixTime();
		if (laststatused.containsKey(url)) {
			final int last=laststatused.get(url);
			if ("admins".equals(url)) {  // bodge, admins get 5 minute pesterings :P
				if ((now-last)>300) {
					laststatused.put(url,now);
					return true;
				}
				return false;
			}
			if ((now-last)>30) {
				laststatused.put(url,now);
				return true;
			}
			return false;
		}
		laststatused.put(url,now);
		return true;
	}

}
