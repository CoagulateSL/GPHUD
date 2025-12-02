package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.DBException;
import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputEmptyException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.EndOfLifing;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Experience.Experience;
import net.coagulate.GPHUD.Modules.Experience.GenericXP;
import net.coagulate.GPHUD.Modules.Experience.QuotaedXP;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.CacheConfig;
import net.coagulate.SL.Config;
import net.coagulate.SL.Data.User;
import net.coagulate.SL.SL;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * Reference to an instance
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Instance extends TableRow {
	
	private static final int                    ADMIN_PESTER_INTERVAL =3600; // seconds
	private static final int                    SERVER_UPDATE_INTERVAL=30;
	private static final Map<String,Integer>    lastStatus            =new TreeMap<>();
			// naughty, static data, but that's okay really for this, ensures we don't spam admins/region servers
	final                Cache<String,Currency> currencyNameCache     =
			Cache.getCache("gphud/instanceCurrency/"+getId(),CacheConfig.PERMANENT_CONFIG,true);
	Cache<String,Attribute> attributeNameResolution=Cache.getCache("gphud/instanceAttributeNameResolution/"+getId(),CacheConfig.PERMANENT_CONFIG,true);
	Cache<String,CharacterGroup> characterGroupResolveCache=Cache.getCache("gphud/instanceCharacterGroupResolveCache/"+getId(),CacheConfig.PERMANENT_CONFIG,true);
	Cache<String,PermissionsGroup>         permissionsGroupResolverCache=Cache.getCache("gphud/instancePermissionsGroupResolver/"+getId(),CacheConfig.PERMANENT_CONFIG,true);
	Cache<String,Item> itemNameResolveCache=Cache.getCache("gphud/instanceItemNameResolver/"+getId(),CacheConfig.PERMANENT_CONFIG,true);
	Cache<String,Menu> menuNameResolveCache=Cache.getCache("gphud/instanceMenuNameResolveCache/"+getId(),CacheConfig.PERMANENT_CONFIG,true);
	Cache<User,Char> mostRecentlyPlayedCache=Cache.getCache("gphud/instanceMostRecentlyPlayed/"+getId(),CacheConfig.PERMANENT_CONFIG);
	
	protected Instance(final int id) {
		super(id);
	}
	
	// ---------- STATICS ----------
	
	/**
	 * Get all the instances.
	 *
	 * @return Set of Instances
	 */
	@Nonnull
	public static Set<Instance> getInstances() {
		final Set<Instance> instances=new TreeSet<>();
		final Results instanceRows=db().dq("select instanceid from instances");
		for (final ResultsRow r: instanceRows) {
			instances.add(Instance.get(r.getInt()));
		}
		return instances;
	}
	
	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return An Instance representation
	 */
	@Nonnull
	public static Instance get(@Nonnull final Integer id) {
		return (Instance)factoryPut("Instance",id,Instance::new);
	}
	
	/**
	 * Create a new instance with a name and owner
	 *
	 * @param name   Name of instance
	 * @param caller Owner of instance
	 * @throws UserInputEmptyException          if the instance name is rejected
	 * @throws UserInputDuplicateValueException if the instance name is already taken
	 */
	public static void create(@Nonnull final String name,@Nonnull final User caller) {
		if (name.isEmpty()) {
			throw new UserInputEmptyException("Can't create null or empty instance");
		}
		final int exists=db().dqiNotNull("select count(*) from instances where name like ?",name);
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
	 * @return Instance object
	 */
	@Nonnull
	public static Instance find(final String name) {
		try {
			final int id=db().dqiNotNull("select instanceid from instances where name=?",name);
			return get(id);
		} catch (@Nonnull final NoDataException e) {
			throw new UserInputLookupFailureException("Unable to find instance named '"+name+"'",e);
		}
	}
	
	/**
	 * Find instance by owner
	 *
	 * @param owner Avatar to find instances for
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
	
	public static void quotaCredits() {
		final int quotaLimit=Config.getQuotaLimit();
		final int quotaInterval=Config.getQuotaInterval();
		for (final ResultsRow row: db().dq("select instanceid from instances where nextquotaincrease<?",
		                                   getUnixTime())) {
			db().d("update instances set reportquota=least(reportquota+1,?),downloadquota=least(downloadquota+1,?),nextquotaincrease=? where instanceid=?",
			       quotaLimit,
			       quotaLimit,
			       getUnixTime()+quotaInterval,
			       row.getInt());
		}
	}
	
	@Nonnull
	public static Set<Instance> getNonRetiringInstances() {
		final Set<Instance> instances=new HashSet<>();
		for (final ResultsRow row: GPHUD.getDB().dq("select instanceid from instances where retireat is null")) {
			instances.add(Instance.get(row.getInt("instanceid")));
		}
		return instances;
	}
	
	@Nonnull
	public static Set<Instance> getWarnableInstances() {
		final Set<Instance> instances=new HashSet<>();
		for (final ResultsRow row: GPHUD.getDB()
		                                .dq("select instanceid from instances where retireat is not null and retirewarn<?",
		                                    getUnixTime())) {
			instances.add(Instance.get(row.getInt("instanceid")));
		}
		return instances;
	}
	
	public static Set<Instance> getExpiredInstances() {
		final Set<Instance> instances=new HashSet<>();
		for (final ResultsRow row: GPHUD.getDB()
		                                .dq("select instanceid from instances where retireat<?",getUnixTime())) {
			instances.add(Instance.get(row.getInt("instanceid")));
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
		return instanceOwnerCache.get(getId(),()->User.get(getInt("owner")));
	}
	
	private static final Cache<Integer,User> instanceOwnerCache=Cache.getCache("GPHUD/InstanceOwner",CacheConfig.PERMANENT_CONFIG);
	
	/**
	 * Set the owner of this instance.
	 *
	 * @param id New owner
	 */
	public void setOwner(@Nonnull final User id) {
		d("update instances set owner=? where instanceid=?",id.getId(),getId());
		instanceOwnerCache.set(getId(),id);
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
		if (validated) {
			return;
		}
		validate();
	}
	
	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}
	
	@Nonnull
	@Override
	public String getLinkTarget() {
		return "instances";
	}
	
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
	
	protected int getNameCacheTime() {
		return 60*60;
	} // this name doesn't change, cache 1 hour
	
	/**
	 * Push updated status to all region server.
	 */
	public void updateStatus() {
		String statusColor="<0.5,1,0.5>";
		int level=0;
		final StringBuilder newStatus=new StringBuilder();
		final Integer minVersion=dqi(
				"select min(regionserverversion) from regions where regionserverversion is not null and instanceid=? and retired=0",
				getId());
		if (minVersion==null) {
			return;
		} // no active instances?
		final Float expiresIn=EndOfLifing.expiresIn(minVersion);
		String updateWithin="";
		if (expiresIn!=null) {
			updateWithin=
					((int)expiresIn.floatValue())+" days "+((int)((expiresIn-((int)expiresIn.floatValue()))*24.0f))+
					" hours";
		}
		String eol="";
		if (expiresIn!=null&&expiresIn>14) {
			eol+="EOL: "+updateWithin;
		}
		if (Config.getDevelopment()) {
			newStatus.append("===DEVELOPMENT===\n \n");
		}
		final String version=SL.getModule("GPHUD").getBuildDateString()+" @"+SL.getModule("GPHUD").commitId();
		newStatus.append("Server: ")
		         .append(Config.getHostName())
		         .append(" - ")
		         .append(version)
		         .append("\n")
		         .append(eol)
		         .append("\n \n");
		if (expiresIn!=null) {
			if (expiresIn<7&&canStatus("expiring-"+getId())) {
				broadcastAdmins(null,
				                "SYSTEM : Alert, this version will be unsupported and marked end of line in "+
				                updateWithin+
				                ".  Please ensure the GPHUD Region Server is upgraded by then to continue your service.");
			}
			if (expiresIn<=14) {
				if (expiresIn<3) {
					newStatus.append("!!!ALERT!!!\nThis version is out of date\nand must be upgraded within\n")
					         .append(updateWithin)
					         .append("\nIt will stop working at this time\n!!!ALERT!!!\n \n");
				} else {
					newStatus.append("Update REQUIRED within ").append(updateWithin).append("\n \n");
				}
			}
		}
		//newStatus+=new Date().toString()+" ";
		for (final Region r: Region.getRegions(this,false)) {
			newStatus.append("[").append(r.getName()).append("#");
			final Integer visitors=r.getOpenVisitCount();
			final String url=dqs("select url from regions where regionid=?",r.getId());
			Integer urlLast=dqi("select urllast from regions where regionid=?",r.getId());
			if (urlLast==null) {
				urlLast=getUnixTime();
			}
			if (url==null||url.isEmpty()) {
				newStatus.append("ERROR:DISCONNECTED]");
				if (canStatus("admins-"+getId())) {
					broadcastAdmins(null,
					                "SYSTEM : Alert, region server for '"+r.getName()+
					                "' is not connected to GPHUD Server.");
				}
				if (level<2) {
					statusColor="<1.0,0.5,0.5>";
					level=2;
				}
			} else {
				if ((getUnixTime()-urlLast)>(15*60)) {
					newStatus.append("*STALLED*");
					if (canStatus("admins-"+getId())) {
						broadcastAdmins(null,
						                "SYSTEM : Alert, region server for '"+r.getName()+
						                "' is not communicating (STALLED / CRASHED)??.");
					}
					if (level<2) {
						statusColor="<1.0,0.5,0.5>";
						level=2;
					}
				}
				newStatus.append(visitors);
				//System.out.println(r+" - "+r.needsUpdate());
				if (r.needsUpdate()) {
					newStatus.append("*UPDATE*");
					if (level<1) {
						statusColor="<1.0,1.0,0.5>";
						level=1;
					}
				}
				newStatus.append("]\n");
				
			}
		}
		newStatus.append(" \n");
		if (expiresIn!=null) {
			if (expiresIn<14) {
				if (expiresIn<3) {
					statusColor="<1.0,0.25,0.25>";
				} else {
					statusColor="<1,0.376,0>";
				}
			}
		}
		final JSONObject statusUpdate=new JSONObject();
		statusUpdate.put("instancestatus",newStatus.toString());
		statusUpdate.put("statuscolor",statusColor);
		for (final Region r: Region.getInstanceNodeRegions(this,false)) {
			final String url=r.getURLNullable();
			if (url!=null) {
				if (canStatus(url)) {
					final Transmission t=new Transmission(r,statusUpdate);
					t.start();
				}
			}
		}
	}
	
	/**
	 * Send a broadcast via the region servers.
	 * This message is encoded as the HUD command "message" causing it to be ownerSay-ed to all users.
	 *
	 * @param message The message to broadcast to all players at the instance.
	 */
	public void broadcastMessage(final String message) {
		GPHUD.getLogger().info("Sending broadcast to instance "+getName()+" - "+message);
		final JSONObject j=new JSONObject();
		JSONResponse.message(j,message,0); //hard wired to the basic protocol ; "message" will always be supported :P
		j.put("incommand","broadcast");
		sendServers(j);
	}
	
	/**
	 * Determine if a message should be sent to the given URL
	 * We rate limit status updates to 1 every 30 seconds for servers.
	 * We use a fake url of "admins" to limit broadcast updates to admins (for server faults).
	 *
	 * @param url The URL of the region server, or "admins" for checking admin updates
	 * @return true if it is permitted to send an update to this target at this time
	 */
	private boolean canStatus(final String url) {
		final int now=getUnixTime();
		if (lastStatus.containsKey(url)) {
			final int last=lastStatus.get(url);
			if (url.startsWith("admins-")||
			    url.startsWith("expiring-")) {  // bodge, admins get weird INTERVAL minute pestering :P
				if ((now-last)>ADMIN_PESTER_INTERVAL) {
					lastStatus.put(url,now);
					return true;
				}
				return false;
			}
			if ("expiring".equals(url)) {  // bodge, admins get 15 minute pestering :P
				if ((now-last)>ADMIN_PESTER_INTERVAL) {
					lastStatus.put(url,now);
					return true;
				}
				return false;
			}
			if ((now-last)>SERVER_UPDATE_INTERVAL) {
				lastStatus.put(url,now);
				return true;
			}
			return false;
		}
		lastStatus.put(url,now);
		return true;
	}
	
	/**
	 * Push a message to all admins of this instance.
	 * Sends message individually to the admins - note this could probably be refitted with the bulk delivery mechanism, on the other hand the number of people getting these
	 * alerts is probably small.
	 *
	 * @param st      Session state
	 * @param message Message to send to admins
	 * @return count of the number of users the message is sent to.
	 */
	public int broadcastAdmins(@Nullable final State st,final String message) {
		final JSONObject j=new JSONObject();
		JSONResponse.message(j,"ADMIN : "+message,0); //hardwired protocol as a singular (spammed) transmission
		final Set<User> targets=new HashSet<>();
		targets.add(getOwner());
		//System.out.println("Pre broadcast!");
		final Results results=
				dq("select avatarid from permissionsgroupmembers,permissionsgroups,permissions where permissionsgroupmembers.permissionsgroupid=permissionsgroups"+
				   ".permissionsgroupid and permissionsgroups.instanceid=? and permissionsgroups.permissionsgroupid=permissions.permissionsgroupid and permission like "+
				   "'instance.receiveadminmessages'",getId());
		for (final ResultsRow r: results) {
			targets.add(User.get(r.getInt()));
		}
		//System.out.println("Avatars:"+targets.size());
		//for (User target:targets) { System.out.println(target.getName()); }
		final Set<Char> chars=new TreeSet<>();
		for (final User a: targets) {
			final Results charList=dq(
					"select characterid from characters where instanceid=? and playedby=? and url is not null",
					getId(),
					a.getId());
			//System.out.println("select characterid from characters where instanceid="+getId()+" and playedby="+a.getId()+" and url is not null;");
			//System.out.println("In chars:"+charList.size());
			for (final ResultsRow rr: charList) {
				chars.add(Char.get(rr.getInt()));
			}
		}
		//System.out.println("Characters:"+chars.size());
		//for (Char target:chars) { System.out.println(target.getName()); }
		for (final Char c: chars) {
			final Transmission t=new Transmission(c,j);
			t.start();
		}
		if (st==null) {
			GPHUD.getLogger(getNameSafe()).info("Sent to "+chars.size()+" admins : "+message);
		} else {
			st.logger().info("Sent to "+chars.size()+" admins : "+message);
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
	 */
	@Nonnull
	public List<CharacterSummary> getCharacterSummary(@Nonnull final State st,
	                                                  @Nonnull final String search,
	                                                  final boolean showRetired) {
		String sortBy=st.getDebasedURL().replaceAll("%20"," ");
		sortBy=sortBy.replaceFirst(".*?sort=","");
		final User searchUser=User.findUsernameNullable(search,false);
		boolean reverse=false;
		//System.out.println(sortBy+" "+reverse);
		if (sortBy.startsWith("-")) {
			reverse=true;
			sortBy=sortBy.substring(1);
		}
		//System.out.println(sortBy+" "+reverse);
		final Map<Integer,CharacterSummary> idMap=new TreeMap<>();
		final List<String> groupHeaders=new ArrayList<>();
		if (st.hasModule("Faction")) {
			groupHeaders.add("Faction");
		}
		for (final ResultsRow r: dq(
				"select name from attributes where instanceid=? and grouptype is not null and attributetype='GROUP'",
				getId())) {
			groupHeaders.add(r.getStringNullable());
		}
		final List<Object> parameters=new ArrayList<>();
		parameters.add(getId());
		String additional="";
		final int lastActive=getUnixTime()-(60*60*24*30*3);
		String charactersScoping="";
		if (!showRetired) {
			charactersScoping=" and characters.retired=0 and characters.lastactive>"+lastActive+" ";
			additional+=charactersScoping;
		}
		if (searchUser!=null||!search.isEmpty()) {
			additional=" and (";
			if (!search.isEmpty()) {
				additional+="name like ? ";
				parameters.add("%"+search+"%");
			}
			if (searchUser!=null&&!search.isEmpty()) {
				additional+=" or ";
			}
			if (searchUser!=null) {
				additional+=" owner="+searchUser.getId();
			}
			additional+=") ";
		}
		for (final ResultsRow r: getDatabase().dqSlow("select * from characters where instanceid=? "+additional,
		                                              parameters.toArray())) {
			final int retired=r.getInt("retired");
			final int charId=r.getInt("characterid");
			final CharacterSummary cr=new CharacterSummary();
			cr.id=charId;
			cr.lastactive=r.getInt("lastactive");
			cr.name=r.getString("name");
			cr.ownerid=r.getInt("owner");
			cr.groupheaders=groupHeaders;
			cr.retired=retired==1;
			cr.online=r.getStringNullable("url")!=null&&!r.getString("url").isEmpty();
			idMap.put(charId,cr);
		}
		for (final ResultsRow r: getDatabase().dqSlow(
				"select charactergroupmembers.characterid,charactergroups.type,charactergroups.name from charactergroupmembers,charactergroups where charactergroupmembers"+
				".charactergroupid=charactergroups.charactergroupid and instanceid=?",
				getId())) {
			final int charId=r.getInt("characterid");
			final String groupType=r.getStringNullable("type");
			final String groupName=r.getStringNullable("name");
			if (idMap.containsKey(charId)) {
				final CharacterSummary cr=idMap.get(charId);
				cr.setGroup(groupType,groupName);
			}
		}
		for (final ResultsRow r: getDatabase().dqSlow(
				"select visits.characterid,sum(endtime-starttime) as totaltime from visits inner join characters on visits.characterid=characters.characterid where characters.instanceid=? "+
				charactersScoping+" and endtime is not null group by characterid",
				getId())) {
			final int id=r.getInt("characterid");
			if (idMap.containsKey(id)) {
				idMap.get(id).totalvisits=r.getInt("totaltime");
			}
		}
		for (final ResultsRow r: getDatabase().dqSlow(
				"select visits.characterid,sum(endtime-starttime) as totaltime from visits  inner join characters on visits.characterid=characters.characterid where characters.instanceid=? "+
				charactersScoping+" and endtime is not null and starttime>? group by characterid",
				getId(),
				getUnixTime()-(Experience.getCycle(st)))) {
			final int id=r.getInt("characterid");
			if (idMap.containsKey(id)) {
				idMap.get(id).recentvisits=r.getInt("totaltime");
			}
		}
		for (final ResultsRow r: getDatabase().dqSlow(
				"select visits.characterid,starttime from visits inner join characters on visits.characterid=characters.characterid where characters.instanceid=? "+
				charactersScoping+" and endtime is null and starttime>?",
				getId(),
				getUnixTime()-(Experience.getCycle(st)))) {
			final int id=r.getInt("characterid");
			final int add=getUnixTime()-r.getInt("starttime");
			if (idMap.containsKey(id)) {
				idMap.get(id).recentvisits=idMap.get(id).recentvisits+add;
				idMap.get(id).totalvisits=idMap.get(id).totalvisits+add;
			}
		}
		for (final ResultsRow r: getDatabase().dqSlow(
				"select characterpools.characterid,sum(adjustment) as total from characterpools inner join characters on characterpools.characterid=characters.characterid where characters.instanceid=? "+
				charactersScoping+
				" and (poolname like 'Experience.%' or poolname like 'Faction.FactionXP' or poolname like 'Events.EventXP') group by "+
				"characterid",
				getId())) {
			final int id=r.getInt("characterid");
			if (idMap.containsKey(id)) {
				idMap.get(id).totalxp=r.getInt("total");
			}
		}
		final Map<Integer,String> avatarNames=User.getIdToNameMap();
		for (final CharacterSummary cs: idMap.values()) {
			cs.ownername=avatarNames.get(cs.ownerid);
		}
		
		final List<CharacterSummary> sortedlist=new ArrayList<>();
		if (sortBy.isEmpty()) {
			sortBy="name";
		}
		sortBy=sortBy.toLowerCase();
		if ("name".equals(sortBy)||"owner".equals(sortBy)) {
			final Map<String,Set<CharacterSummary>> sorted=new TreeMap<>();
			for (final CharacterSummary cs: idMap.values()) {
				String value=cs.name;
				if ("owner".equals(sortBy)) {
					value=cs.ownername;
				}
				Set<CharacterSummary> records=new HashSet<>();
				if (sorted.containsKey(value)) {
					records=sorted.get(value);
				}
				records.add(cs);
				sorted.put(value,records);
			}
			final List<String> sortedKeys=new ArrayList<>(sorted.keySet());
			if (reverse) {
				sortedKeys.sort(Collections.reverseOrder());
			} else {
				Collections.sort(sortedKeys);
			}
			for (final String key: sortedKeys) {
				final Set<CharacterSummary> set=sorted.get(key);
				sortedlist.addAll(set);
			}
		} else {
			final Map<Integer,Set<CharacterSummary>> sorted=new TreeMap<>();
			for (final CharacterSummary cs: idMap.values()) {
				int value=cs.lastactive;
				if ("total visit time (3 months)".equals(sortBy)) {
					value=cs.totalvisits;
				}
				if (sortBy.equals("visit time (last "+Experience.getCycleLabel(st).toLowerCase()+")")) {
					value=cs.recentvisits;
				}
				if ("total xp".equals(sortBy)) {
					value=cs.totalxp;
				}
				if ("level".equals(sortBy)) {
					value=cs.totalxp;
				}
				
				Set<CharacterSummary> records=new TreeSet<>();
				if (sorted.containsKey(value)) {
					records=sorted.get(value);
				}
				records.add(cs);
				sorted.put(value,records);
			}
			final List<Integer> sortedKeys=new ArrayList<>(sorted.keySet());
			if (reverse) {
				Collections.sort(sortedKeys);
			} // note reverse is reversed for numbers
			else {
				sortedKeys.sort(Collections.reverseOrder());
			}
			// default is biggest at top, smallest at bottom, which is reverse order as the NORMAL order.   alphabetic is a-z so forward order for the NORMAL order....
			for (final Integer key: sortedKeys) {
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
	 * @return Set of character groups (potentially the empty set)
	 */
	@Nonnull
	public Set<CharacterGroup> getGroupsForKeyword(final String keyword) {
		final Set<CharacterGroup> groups=new TreeSet<>();
		for (final ResultsRow r: dq("select charactergroupid from charactergroups where instanceid=? and type=?",
		                            getId(),
		                            keyword)) {
			groups.add(CharacterGroup.get(r.getInt()));
		}
		return groups;
	}
	
	/**
	 * Get all character groups for this instance.
	 * Ignores the group type element.
	 *
	 * @return Listr of CharacterGroups
	 */
	@Nonnull
	public List<CharacterGroup> getCharacterGroups() {
		return CharacterGroup.getInstanceGroups(this);
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
	 * Create a character group.
	 *
	 * @param name    Name of the group to create
	 * @param open    Is the group open to join (otherwise invite only)?
	 * @param keyword Type of the group, optionally.
	 * @throws UserException If the group can not be created, already exists, etc.
	 */
	public void createCharacterGroup(final String name,final boolean open,final String keyword) {
		CharacterGroup.create(this,name,open,keyword);
	}
	
	/**
	 * Recompute all possibly conveyances for all logged in characters.
	 * Bulk update via server dissemination where necessary.
	 */
	public void pushConveyances() {
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
					final User user=c.getPlayedByNullable();
					String playedby=null;
					if (user!=null) {
						playedby=user.getUUID();
					}
					if (playedby==null) { // maybe its an object
						try {
							playedby=dqs("select uuid from objects where url like ?",c.getURL());
						} catch (@Nonnull final NoDataException ignored) {
						}
					}
					if (playedby!=null) {
						final String payloadString=payload.toString();
						buffer.get(reg).put(playedby,payloadString);
						if (buffer.get(reg).toString().length()>(1024+512+256)) {
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
	 * Get the calculated width of this logo
	 *
	 * @param height Given height of the logo
	 * @return Width, calculated by scaling with GPHUDClient.widthMultiplier * height
	 */
	public int getLogoWidth(final float height) {
		final State fakeState=new State(this);
		final float multiplier=fakeState.getKV("GPHUDClient.widthMultiplier").floatValue();
		return (int)(height*multiplier);
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
	 * Get this instance level logo
	 *
	 * @param st State to get instance from
	 * @return A String reference to the SL texture service's URL for the logo, or a reference to banner-gphud.png
	 */
	@Nonnull
	public String getLogoURL(@Nonnull final State st) {
		final String logoUuid=st.getKV(this,"GPHUDClient.logo");
		if (logoUuid==null||logoUuid.isEmpty()) {
			return "/resources/banner-gphud.png";
		}
		return SL.textureURL(logoUuid);
	}
	
	public Set<Char> getCharacters() {
		final Set<Char> chars=new HashSet<>();
		for (final ResultsRow row: dq("select characterid from characters where instanceid=?",getId())) {
			chars.add(Char.get(row.getInt()));
		}
		return chars;
	}
	
	public boolean spendReportCredit() {
		final int quota=getInt("reportquota");
		if (quota>0) {
			set("reportquota",quota-1);
			return true;
		}
		return false;
	}
	
	public int countCharacters() {
		return dqinn("select count(*) from characters where instanceid=?",getId());
	}
	
	public int reportCredits() {
		return getInt("reportquota");
	}
	
	public int downloadCredits() {
		return getInt("downloadquota");
	}
	
	
	public boolean spendDownloadCredit() {
		final int quota=getInt("downloadquota");
		if (quota>0) {
			set("downloadquota",quota-1);
			return true;
		}
		return false;
	}
	
	public void startReport() {
		d("update characters set reported=0 where instanceid=?",getId());
		d("delete from reports where instanceid=?",getId());
		set("reportstart",UnixTime.getUnixTime());
		set("reporting",true);
	}
	
	public void endReport() {
		set("reporting",false);
		set("reportend",UnixTime.getUnixTime());
	}
	
	public int reportingStartTime() {
		return getInt("reportstart");
	}
	
	public int reportingEndTime() {
		return getInt("reportend");
	}
	
	public int countReportedCharacters() {
		return dqinn("select count(distinct characterid) from reports where instanceid=?",getId());
	}
	
	public boolean reporting() {
		return getBool("reporting");
	}
	
	public void setRetireAt(@Nullable final Integer retireat) {
		set("retireat",retireat);
	}
	
	@Nullable
	public Integer retireAt() {
		return getIntNullable("retireat");
	}
	
	public void setRetireWarn(@Nullable final Integer warnAt) {
		set("retirewarn",warnAt);
	}
	
	@Nullable
	public Integer retireWarn() {
		return getIntNullable("retirewarn");
	}
	
	public void delete() {
		d("delete from instances where instanceid=?",getId());
	}
	
	public static void reportingTick(final Logger log) {
		GPHUD.getDB().dq("select instanceid from instances where reporting=1").forEach(x->{
			Instance i=Instance.get(x.getInt());
			// grab a character
			try {
				Integer charid=GPHUD.getDB()
				                    .dqi("SELECT characterid FROM characters WHERE instanceid=? and reported=0 limit 0,1",i.getId());
				if (charid==null) {
					i.endReport();
				} else {
					try {
						GPHUD.getDB().d("UPDATE characters SET reported=1 WHERE characterid=?",charid);
						reportCharacter(log,i,Char.get(charid));
					} catch (RuntimeException e) {
						log.log(Level.WARNING,"Error generating report for character "+charid+" in instance "+i,e);
					}
				}
			} catch (NoDataException done) { i.endReport(); }
		});
	}
	
	static void reportCharacter(final Logger log, final Instance instance,final Char character) {
		// we basically make a KV map then write it as a series of rows
		final State charState=new State(character);
		Map<String,String> output=new HashMap<>();
		output.put("ID",Integer.toString(character.getId()));
		output.put("Character Name",character.getName());
		output.put("Retired",Boolean.toString(character.retired()));
		output.put("Owner Name",character.getOwner().getName());
		output.put("Last Active (SLT)",UnixTime.fromUnixTime(character.getLastPlayed(),"America/Los_Angeles"));
		for (final Attribute attribute: charState.getAttributes()) {
			String value="EXCEPTION";
			try {
				switch (attribute.getType()) {
					case SET -> value=new CharacterSet(character,attribute).textList();
					case POOL -> {
						if (QuotaedXP.class.isAssignableFrom(attribute.getClass())) {
							final QuotaedXP xp=(QuotaedXP)attribute;
							value=String.valueOf(CharacterPool.sumPool(charState,(xp.getPool(charState))));
						} else {
							value="SomeKindOfPool (?)";
						}
					}
					case GROUP -> {
						final String subType=attribute.getSubType();
						if (subType==null) {
							value="";
						} else {
							final CharacterGroup groupMembership=CharacterGroup.getGroup(character,subType);
							value=(groupMembership==null?"":groupMembership.getName());
						}
					}
					case CURRENCY -> value=String.valueOf((Currency.find(charState,attribute.getName()).sum(charState)));
					case INVENTORY -> value=(new Inventory(character,attribute).textList());
					case EXPERIENCE -> value=String.valueOf((CharacterPool.sumPool(charState,(new GenericXP(attribute.getName()).getPool(charState)))));
					case COLOR,INTEGER,TEXT,FLOAT ->
							value=(charState.getKV("Characters."+attribute.getName()).toString());
				}
			} catch (final Exception e) {
				SL.log("Reporting").log(Level.WARNING,"Exception reporting attribute "+attribute.getName()+" for char "+character.getName()+"#"+character.getId()+": "+e,e);
			}
			output.put(attribute.getName(),value);
		}
		final int xp=Experience.getExperience(charState,character);
		output.put("Total XP",String.valueOf(xp));
		output.put("Level",String.valueOf(Experience.toLevel(charState,xp)));
		try {
			// batch insert
			Object[] params=new Object[4*output.size()];
			StringBuilder sql=
					new StringBuilder("INSERT INTO reports(`instanceid`,`characterid`,`column`,`data`) VALUES ");
			boolean isfirst=true;
			int index=0;
			for (Map.Entry<String,String> e: output.entrySet()) {
				if (isfirst) {
					isfirst=false;
				} else {
					sql.append(",");
				}
				sql.append("(?,?,?,?)");
				params[index*4]=instance.getId();
				params[index*4+1]=character.getId();
				params[index*4+2]=e.getKey();
				params[index*4+3]=e.getValue();
				index++;
			}
			db().d(sql.toString(),params);
		} catch (DBException e) {
			// fine then, try it one by one
			for (Map.Entry<String,String> entry: output.entrySet()) {
				try {
					db().d("INSERT INTO reports(`instanceid`,`characterid`,`column`,`data`) VALUES(?,?,?,?)",instance.getId(),character.getId(),entry.getKey(),entry.getValue());
				} catch (DBException f) {
					log.log(Level.WARNING,"Unable to insert report result for "+instance+" char "+character+" entry "+entry.getKey()+"="+entry.getValue(),f);
					try {db().d("INSERT INTO reports(`instanceid`,`characterid`,`column`,`data`) VALUES(?,?,?,'EXCEPTION')",instance.getId(),character.getId(),entry.getKey()); }
					catch (DBException g) {
						log.log(Level.WARNING,"Unable to insert report result for "+instance+" char "+character+" entry "+entry.getKey()+" - could not even write that this was EXCEPTION",g);
					}
				}
			}
		}
	}
	
	public String getReport() {
		try {
			final StringWriter output=new StringWriter();
			final CSVPrinter csv=new CSVPrinter(output,CSVFormat.EXCEL);
			Results columns=dq("SELECT DISTINCT `column` FROM reports where instanceid=?",getId());
			String[] headers=new String[columns.size()];
			final AtomicInteger index=new AtomicInteger(0);
			columns.forEach(header->{
				headers[index.getAndIncrement()]=header.getString();
			});
			for (String header:headers) { csv.print(header); }
			csv.println();
			// for all characters
			dq("SELECT DISTINCT characterid FROM reports where instanceid=?",getId()).forEach(charidrow->{
				try {
					Map<String,String> charmap=new HashMap<>();
					dq("SELECT `column`,`data` FROM reports where `characterid`=?",charidrow.getInt()).forEach(row->{
						charmap.put(row.getString("column"),row.getString("data"));
					});
					for (String header: headers) {
						csv.print(charmap.getOrDefault(header,""));
					}
					csv.println();
				} catch (IOException f) { throw new SystemImplementationException("Unexpected IO Exception generating report CSV per char",f); }
			});
			return output.toString();
		} catch (IOException e) { throw new SystemImplementationException("Unexpected IO Exception generating report CSV",e); }
	}
}
