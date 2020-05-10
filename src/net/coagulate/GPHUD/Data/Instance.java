package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputEmptyException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
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

	private static final int ADMIN_PESTER_INTERVAL=900; // seconds
	private static final int SERVER_UPDATE_INTERVAL=30;
	private static final Map<String,Integer> laststatused=new TreeMap<>(); // naughty, static data, but thats okay really for this, ensures we dont spam admins/region servers

	protected Instance(final int id) { super(id); }

	// ---------- STATICS ----------

	/**
	 * Get all the instances.
	 *
	 * @return Set of Instances
	 */
	@Nonnull
	public static Set<Instance> getInstances() {
		final Set<Instance> instances=new TreeSet<>();
		final Results instancerows=db().dq("select instanceid from instances");
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

	/**
	 * Create a new instance with a name and owner
	 *
	 * @param name   Name of instance
	 * @param caller Owner of instance
	 *
	 * @return Blank string on success, otherwise error hudMessage
	 *
	 * @throws UserInputEmptyException          if the instance name is rejected
	 * @throws UserInputDuplicateValueException if the instance name is already taken
	 */
	public static void create(@Nonnull final String name,
	                          @Nonnull final User caller) {
		if ("".equals(name)) { throw new UserInputEmptyException("Can't create null or empty instance"); }
		final int exists=db().dqinn("select count(*) from instances where name like ?",name);
		if (exists!=0) {
			throw new UserInputDuplicateValueException("Instance already exists!");
		}
		GPHUD.getLogger().info(caller.getName()+" created new instance '"+name+"'");
		db().d("insert into instances(owner,name) value(?,?)",caller.getId(),name);
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
			final int id=db().dqinn("select instanceid from instances where name=?",name);
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
		final Results results=db().dq("select instanceid from instances where owner=?",owner.getId());
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
	 * Push updated status to all region server.
	 */
	public void updateStatus() {
		String statuscolor="<0.5,1,0.5>";
		int level=0;
		final StringBuilder newstatus=new StringBuilder();
		if (GPHUD.DEV) { newstatus.append("===DEVELOPMENT===\n \n"); }
		newstatus.append("Server: ").append(GPHUD.hostname).append(" - ").append(GPHUD.VERSION).append("\n \n");
		//newstatus+=new Date().toString()+" ";
		for (final Region r: Region.getRegions(this,false)) {
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
		for (final Region r: Region.getInstanceNodeRegions(this,false)) {
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
	public List<CharacterSummary> getCharacterSummary(@Nonnull final State st,
	                                                  @Nonnull final String search) {
		String sortby=st.getDebasedURL().replaceAll("%20"," ");
		sortby=sortby.replaceFirst(".*?sort=","");
		User searchuser=User.findUsernameNullable(search,false);
		Char searchchar=Char.findNullable(st.getInstance(),search);
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
		String additional="";
		if (searchuser!=null || searchchar!=null) {
			additional=" and (";
			if (searchchar!=null) {
				additional+="characterid="+searchchar.getId();
			}
			if (searchuser!=null && searchchar!=null) {
				additional+=" or ";
			}
			if (searchuser!=null) {
				additional+=" owner="+searchuser.getId();
			}
			additional+=") ";
		}
		for (final ResultsRow r: dq("select * from characters where instanceid=? "+additional,getId())) {
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
		for (final Region r: Region.getRegions(this,false)) {
			r.sendServer(j);
			//System.out.println("Send to "+r.getName()+" "+j.toString());
		}
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

	/**
	 * Get this instance level logo
	 *
	 * @param st State to get instance from
	 *
	 * @return A String reference to the SL texture service's URL for the logo, or a reference to banner-gphud.png
	 */
	@Nonnull
	public String getLogoURL(@Nonnull final State st) {
		final String logouuid=st.getKV(this,"GPHUDClient.logo");
		if (logouuid==null || logouuid.isEmpty()) { return "/resources/banner-gphud.png"; }
		return SL.textureURL(logouuid);
	}

	/**
	 * Get the calculated width of this logo
	 *
	 * @param st     State
	 * @param height Given height of the logo
	 *
	 * @return Width, calculated by scaling with GPHUDclient.widthmultiplier * height
	 */
	public int getLogoWidth(@Nonnull final State st,
	                        final float height) {
		final State fakestate=new State(this);
		final float multiplier=fakestate.getKV("GPHUDClient.widthmultiplier").floatValue();
		return (int) (height*multiplier);
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
			if ("admins".equals(url)) {  // bodge, admins get 15 minute pesterings :P
				if ((now-last)>ADMIN_PESTER_INTERVAL) {
					laststatused.put(url,now);
					return true;
				}
				return false;
			}
			if ((now-last)>SERVER_UPDATE_INTERVAL) {
				laststatused.put(url,now);
				return true;
			}
			return false;
		}
		laststatused.put(url,now);
		return true;
	}

}
