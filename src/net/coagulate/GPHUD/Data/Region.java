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
import net.coagulate.Core.Tools.Cache;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Interface;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Maintenance;
import net.coagulate.GPHUD.Modules.Experience.VisitXP;
import net.coagulate.GPHUD.Modules.Zoning.ZoneTransport;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Config;
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
	
	private static final Cache<Region,Boolean> retiredCache=
			Cache.getCache("gphud/regionretired",CacheConfig.OPERATIONAL_CONFIG);
	
	// ---------- STATICS ----------
	
	/**
	 * Get all the regions associated with this instance bound to this server
	 *
	 * @param instance     The instance to query
	 * @param allowretired Allow retired regions?
	 * @return Set of Regions
	 */
	@Nonnull
	public static Set<Region> getInstanceNodeRegions(@Nonnull final Instance instance,final boolean allowretired) {
		final Results results=db().dq("select regionid from regions where instanceid=? and authnode=? and retired<?",
		                              instance.getId(),
		                              Interface.getNode(),
		                              allowretired?2:1);
		final Set<Region> regions=new TreeSet<>();
		for (final ResultsRow row: results) {
			regions.add(Region.get(row.getInt("regionid"),allowretired));
		}
		return regions;
	}
	
	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A Region representation
	 */
	@Nonnull
	public static Region get(final int id,final boolean allowretired) {
		final Region r=(Region)factoryPut("Region",id,Region::new);
		if (r.isRetired()&&(!allowretired)) {
			final UserException exception=
					new UserInputStateException("Attempt to access retired region "+r.getName(),true);
			GPHUD.getLogger("Regions").log(WARNING,"Attempt to access retired region",exception);
			throw exception;
		}
		return r;
	}
	
	protected Region(final int id) {
		super(id);
	}
	
	/**
	 * Is this region retired
	 *
	 * @return retirement flag
	 */
	public boolean isRetired() {
		return retiredCache.get(this,()->getBool("retired"));
	}
	
	/**
	 * Update the last used time of a URL.
	 *
	 * @param url URL to refresh
	 */
	public static void refreshURL(@Nonnull final String url) {
		final String t="regions";
		final int refreshifolderthan=getUnixTime()-TableRow.REFRESH_INTERVAL;
		final int toupdate=
				db().dqiNotNull("select count(*) from "+t+" where url=? and urllast<?",url,refreshifolderthan);
		if (toupdate==0) {
			return;
		}
		if (toupdate>1) {
			GPHUD.getLogger().warning("Unexpected anomoly, "+toupdate+" rows to update on "+t+" url "+url);
		}
		//Log.log(Log.DEBUG,"SYSTEM","DB_Region","Refreshing REGION url "+url);
		db().d("update "+t+" set urllast=?,authnode=? where url=?",getUnixTime(),Interface.getNode(),url);
	}
	
	@Nonnull
	public static Region find(@Nonnull final String name,final boolean allowretired) {
		final Region r=findNullable(name,allowretired);
		if (r==null) {
			throw new UserInputLookupFailureException("No active region named '"+name+"' found");
		}
		return r;
	}
	
	private static final Cache<String,Integer> regionNameCache=Cache.getCache("GPHUD/RegionNameLookup",CacheConfig.PERMANENT_CONFIG,true);

	/**
	 * Find a region by name.
	 *
	 * @param name Name of region to locate
	 * @return Region object for that region, or null if none is found.
	 */
	@Nullable
	public static Region findNullable(@Nonnull final String name,final boolean allowretired) {
		final Integer regionId=regionNameCache.get(name,()->{
		try {
			final int regionid=db().dqiNotNull("select regionid from regions where name=?",name);
			return regionid;
		} catch (@Nonnull final NoDataException e) {
			return null;
		}});
		if (regionId==null) { return null; }
		return get(regionId,allowretired);
	}

	/**
	 * Register a region against an instance.
	 *
	 * @param region Name of region to register
	 * @param i      Instance object to register the region with
	 * @return A blank string on success, or a text hudMessage explaining any problem.
	 */
	@Nonnull
	public static String joinInstance(@Nonnull final String region,@Nonnull final Instance i) {
		// TO DO - lacks validation
		final int exists=db().dqiNotNull("select count(*) from regions where name=?",region);
		if (exists==0) {
			GPHUD.getLogger().info("Joined region '"+region+"' to instance "+i);
			db().d("insert into regions(name,instanceid) values(?,?)",region,i.getId());
			db().d("update instances set retireat=null,retirewarn=null where instanceid=?",i.getId());
			regionNameCache.purge(region);
			return "";
		}
		return "Region is already registered!";
	}
	
	/**
	 * Returns a set of servers that need pinging
	 * TODO stop returning results to whoever
	 *
	 * @return Results containing region data to be refreshed.
	 */
	@Nonnull
	public static Results getPingable() {
		return db().dq(
				"select regionid,name,url,urllast from regions where url is not null and url!='' and authnode like ? and urllast<? order by urllast "+
				"asc limit 0,"+"30",
				Interface.getNode(),
				getUnixTime()-(Maintenance.PINGSERVERINTERVAL*60));
	}
	
	/**
	 * Get all the regions associated with this instance
	 *
	 * @param st           the state to query
	 * @param allowretired Return retired instances or not
	 * @return Set of Regions
	 */
	@Nonnull
	public static Set<Region> getRegions(@Nonnull final State st,final boolean allowretired) {
		return getRegions(st.getInstance(),allowretired);
	}
	
	// ----- Internal Statics -----
	
	/**
	 * Get all the regions associated with this instance
	 *
	 * @param instance     The instance to get regions for
	 * @param allowretired Return retired instances or not
	 * @return Set of Regions
	 */
	@Nonnull
	public static Set<Region> getRegions(@Nonnull final Instance instance,final boolean allowretired) {
		final Results results=db().dq("select regionid from regions where instanceid=? and retired<?",
		                              instance.getId(),
		                              allowretired?2:1);
		final Set<Region> regions=new TreeSet<>();
		for (final ResultsRow row: results) {
			regions.add(Region.get(row.getInt("regionid"),allowretired));
		}
		return regions;
	}
	
	public static Table statusDump(final State st) {
		final Table t=new Table().border();
		t.header("Region ID");
		t.header("Name");
		t.header("URL");
		t.header("URL Last Active (approx)");
		t.header("Servicing Server");
		t.header("Server Version");
		t.header("Server DateTime");
		t.header("HUD Version");
		t.header("HUD DateTime");
		t.header("Region X");
		t.header("Region Y");
		t.header("Retired");
		t.header("Protocol");
		for (final ResultsRow row: db().dq("select * from regions where instanceid=?",st.getInstance().getId())) {
			t.openRow();
			t.add(row.getIntNullable("regionid"));
			t.add(row.getStringNullable("name"));
			t.add(row.getStringNullable("url")!=null?"Present":"");
			t.add(fromUnixTime(row.getIntNullable("urllast"),st.getAvatar().getTimeZone()));
			t.add(row.getStringNullable("authnode"));
			t.add(row.getIntNullable("regionserverversion"));
			t.add(fromUnixTime(row.getIntNullable("regionserverdatetime"),st.getAvatar().getTimeZone()));
			t.add(row.getIntNullable("regionhudversion"));
			t.add(fromUnixTime(row.getIntNullable("regionhuddatetime"),st.getAvatar().getTimeZone()));
			t.add(row.getIntNullable("regionx"));
			t.add(row.getIntNullable("regiony"));
			Integer retired=row.getIntNullable("retired");
			if (retired==null) {
				retired=0;
			}
			t.add(retired==0?"":"Retired");
			t.add(row.getInt("protocol"));
		}
		return t;
	}
	
	/** Returns active instances ; that is instances with an active region */
	public static Set<Instance> getActiveInstances() {
		final Set<Instance> activeInstances=new HashSet<>();
		for (final ResultsRow row: GPHUD.getDB().dq("select distinct instanceid from regions where retired=0")) {
			activeInstances.add(Instance.get(row.getInt("instanceid")));
		}
		return activeInstances;
	}
	
	/**
	 * Wipe the region KV store of a particular K
	 *
	 * @param instance Instance to wipe KV store for
	 * @param key      K to wipe
	 */
	static void wipeKV(@Nonnull final Instance instance,@Nonnull final String key) {
		final String kvtable="regionkvstore";
		final String maintable="regions";
		final String idcolumn="regionid";
		db().d("delete from "+kvtable+" using "+kvtable+","+maintable+" where "+kvtable+".k like ? and "+kvtable+"."+
		       idcolumn+"="+maintable+"."+idcolumn+" and "+maintable+".instanceid=?",key,instance.getId());
	}
	
	public static Set<Region> getTimedOut() {
		final Set<Region> set=new HashSet<>();
		for (final ResultsRow row: GPHUD.getDB()
		                                .dq("select regionid from regions where retired=0 and urllast<?",
		                                    getUnixTime()-Config.getGPHUDRegionTimeout())) {
			set.add(Region.get(row.getInt("regionid"),true));
		}
		return set;
	}
	
	public static String getLatestVersionString() {
		int maxversion=db().dqOne("select max(regionserverversion) as version from regions").getInt("version");
		final int major=(int)(Math.floor(maxversion/10000));
		maxversion=maxversion-(major*10000);
		final int minor=(int)(Math.floor(maxversion/100));
		final int bugfix=maxversion-(100*minor);
		final String smajor=String.valueOf(major);
		String sminor=String.valueOf(minor);
		String sbugfix=String.valueOf(bugfix);
		if (sminor.length()==1) {
			sminor="0"+sminor;
		}
		if (sbugfix.length()==1) {
			sbugfix="0"+sbugfix;
		}
		return smajor+"."+sminor+"."+sbugfix;
	}
	
	private static final Cache<Integer,Instance> regionToInstanceCache=Cache.getCache("GPHUD/RegionInstanceCache",CacheConfig.PERMANENT_CONFIG);

	/**
	 * Gets the instance associated with this region
	 *
	 * @return The Instance object
	 */
	@Nonnull
	public Instance getInstance() {
		return regionToInstanceCache.get(getId(),()->Instance.get(getInt("instanceid")));
	}
	
	@Nonnull
	@Override
	public String getTableName() {
		return "regions";
	}
	
	@Nonnull
	@Override
	public String getIdColumn() {
		return "regionid";
	}
	
	public void validate(@Nonnull final State st) {
		if (validated) {
			return;
		}
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Region / State Instance mismatch");
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
		return "regions";
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
	
	public String getPrimUUID() {
		return getStringNullable("primuuid");
	}
	
	public void setPrimUUID(final String objectkey) {
		if (!objectkey.equalsIgnoreCase(getPrimUUID())) {
			d("update regions set primuuid=? where regionid=?",objectkey,getId());
		}
		final int now=getUnixTime();
		if ((now-getURLLast())>60) {
			d("update regions set urllast=?,authnode=? where regionid=?",now,Interface.getNode(),getId());
		}
	}
	
	protected int getNameCacheTime() {
		return 60*60;
	} // this name doesn't change, cache 1 hour
	
	public static final Cache<Integer,String> regionURLCache=Cache.getCache("GPHUD/regionURLCache",CacheConfig.PERMANENT_CONFIG);
	
	/**
	 * Sets the region's URL to callback to the server - CALL THIS FREQUENTLY.
	 * This does not /just/ set the region's url, it checks if it needs to be set firstly, it also updates it if and only if necessary, along with shutting down the URL it
	 * replaced.
	 * It also updates the "urllast used" timer if it's more than 60 seconds old.  (used for 'is alive' and 'ping' checking etc etc).
	 *
	 * @param url Targets URL
	 */
	public void setURL(final String url) {
		final String oldurl=getURLNullable();
		final int now=getUnixTime();
		
		if (oldurl!=null&&oldurl.equals(url)) {
			if ((now-getURLLast())>60) {
				d("update regions set urllast=?,authnode=? where regionid=?",now,Interface.getNode(),getId());
			}
			return;
		}
		
		if (oldurl!=null&&!(oldurl.isEmpty())) {
			GPHUD.getLogger().info("Sending shutdown to old URL : "+oldurl);
			final JSONObject tx=new JSONObject().put("incommand","shutdown")
			                                    .put("shutdown","Connection replaced by new region server");
			final Transmission t=new Transmission(this,tx,oldurl);
			t.start();
		}
		regionURLCache.set(getId(),url);
		d("update regions set url=?, urllast=?, authnode=? where regionid=?",url,now,Interface.getNode(),getId());
		
	}
	
	/**
	 * Gets the URL associated with this region's server
	 *
	 * @return the URL, or user errors
	 *
	 * @throws UserRemoteFailureException If the region does not have a URL
	 */
	@Nonnull
	public String getURL() {
		final String url=getURLNullable();
		if (url==null) {
			throw new UserRemoteFailureException("This region server is not operational (has no callback URL)");
		}
		return url;
	}
	
	/**
	 * Gets the URL associated with this region's server
	 *
	 * @return URL
	 */
	@Nullable
	public String getURLNullable() {
		return regionURLCache.get(getId(),()->getStringNullable("url"));
	}
	
	/**
	 * Gets the UNIX time the region's server last checked in.
	 *
	 * @return UnixTime the last time this server's url was refreshed / used
	 */
	public int getURLLast() {
		return getInt("urllast");
	}
	
	/**
	 * Update the visits noting the following avatars left the sim.
	 *
	 * @param st      State
	 * @param avatars List of Avatar UUIDs or Names that have left the sim.
	 */
	public void departingAvatars(@Nonnull final State st,@Nonnull final Set<User> avatars) {
		for (final User avatar: avatars) {
			// for all the departing avatars
			try {
				final int avatarid=avatar.getId();
				// if the avatar exists, see if there's a visit
				final Results rows=dq(
						"select characterid from visits where avatarid=? and regionid=? and endtime is null",
						avatarid,
						getId());
				final int count=rows.size();
				if (count>0) {
					st.logger().info("Disconnected avatar "+avatar.getName());
					d("update eventvisits inner join characters on eventvisits.characterid=characters.characterid set eventvisits.endtime=UNIX_TIMESTAMP() where characters"+
					  ".owner=? and characters.instanceid=?",avatarid,st.getInstance().getId());
					d("update visits set endtime=? where endtime is null and regionid=? and avatarid=?",
					  getUnixTime(),
					  getId(),
					  avatarid);
				}
				// compute visit XP ((TODO REFACTOR ME?))
				for (final ResultsRow r: rows) {
					final State temp=new State();
					temp.setInstance(st.getInstance());
					temp.setCharacter(Char.get(r.getInt("characterid")));
					new VisitXP(-1).runAwards(st,temp.getCharacter());
				}
				final int instanceid=getInstance().getId();
				final Results urls=
						dq("select url from characters where instanceid=? and playedby=? and url is not null",
						   instanceid,
						   avatarid);
				// if the visitor (character) has URLs send them a ping, which will probably 404 and remove its self
				for (final ResultsRow row: urls) {
					final String url=row.getStringNullable("url");
					final JSONObject ping=new JSONObject().put("incommand","ping");
					final Transmission t=new Transmission(null,ping,url,5);
					t.start();
				}
			} catch (@Nonnull final Exception e) {
				st.logger().log(SEVERE,"Exception in departingAvatars",e);
			}
		}
	}
	
	/**
	 * Convenience method that logs a HUD version
	 *
	 * @param st          State
	 * @param version     Version "XX.YY.ZZ"
	 * @param versiondate Preprocessor macro _DATE_ in Firestorm
	 * @param versiontime Preprocessor macro _TIME_ in Firestorm
	 */
	public void recordHUDVersion(@Nonnull final State st,
	                             @Nonnull final String version,
	                             @Nonnull final String versiondate,
	                             @Nonnull final String versiontime,
	                             final int protocol) {
		recordVersion(st,"hud",version,versiondate,versiontime,protocol);
	}
	
	/**
	 * Log a product's version information.
	 *
	 * @param st          State
	 * @param type        Type of product (hud, server)
	 * @param version     Version string (XX.YY.ZZ format) NOTE XX/YY/ZZ should not exceed 2 digits (as it's stored literally as XXYYZZ integer)
	 * @param versiondate Parsable date (see FireStorm preprocessor macro __DATE__)
	 * @param versiontime Parsable time (see FireStorm preprocessor macro __TIME__)
	 */
	private void recordVersion(@Nonnull final State st,
	                           @Nonnull final String type,
	                           @Nonnull final String version,
	                           @Nonnull final String versiondate,
	                           @Nonnull final String versiontime,
	                           int protocol) { // may be overwritten to avoid logging HUD versions in REGIONS table (reserved for server version)
		final Date d;
		try {
			final SimpleDateFormat df=new SimpleDateFormat("MMM d yyyy HH:mm:ss");
			df.setLenient(true);
			String datetime=versiondate+" "+versiontime;
			datetime=datetime.replaceAll(" {2}"," ");
			d=df.parse(datetime);
		} catch (@Nonnull final ParseException ex) {
			throw new SystemImplementationException("Failed to parse date time from "+versiondate+" "+versiontime,ex);
		}
		final ResultsRow regiondata=
				dqone("select region"+type+"version,region"+type+"datetime from regions where regionid=?",getId());
		final Integer oldversion=regiondata.getIntNullable("region"+type+"version");
		final Integer olddatetime=regiondata.getIntNullable("region"+type+"datetime");
		final int newversion=Interface.convertVersion(version);
		final int newdatetime=(int)(d.getTime()/1000.0);
		final int oldprotocol=protocol();
		if ("hud".equalsIgnoreCase(type)) {
			protocol=oldprotocol;
		} // we don't log this in the regions table, just the region server protocol
		if (oldversion==null||olddatetime==null||olddatetime<newdatetime||oldversion<newversion||protocol>oldprotocol) {
			d("update regions set region"+type+"version=?,region"+type+"datetime=?,protocol=? where regionid=?",
			  newversion,
			  newdatetime,
			  protocol,
			  getId());
			final String olddesc=formatVersion(oldversion,olddatetime,false);
			final String newdesc=formatVersion(newversion,newdatetime,false);
			st.logger()
			  .info("Version upgrade of "+type+" from "+olddesc+" to "+newdesc+" protocol "+oldprotocol+" to "+
			        protocol);
			final State fake=new State();
			fake.setInstance(st.getInstance());
			fake.setAvatar(User.getSystem());
			final String updown=(olddatetime==null||olddatetime<newdatetime?"Upgrade":"Downgrade");
			Audit.audit(fake,Audit.OPERATOR.AVATAR,null,null,updown,type,olddesc,newdesc,"Product version "+updown);
		}
	}
	
	/**
	 * Return the open visits to this region.
	 *
	 * @return Set of Characters currently visiting this region.
	 */
	@Nonnull
	public Set<Char> getOpenVisits() {
		final Set<Char> characters=new TreeSet<>();
		final Results results=dq("select characterid from visits where regionid=? and endtime is null",getId());
		for (final ResultsRow r: results) {
			characters.add(Char.get(r.getInt("characterid")));
		}
		return characters;
	}
	
	public int protocol() {
		return getInt("protocol");
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
	private String formatVersion(@Nullable final Integer version,@Nullable final Integer datetime,final boolean html) {
		String v="";
		final DateFormat df=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.MEDIUM);
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		if (version==null) {
			v+="v??? ";
		} else {
			v=v+"v"+(version/10000)+"."+((version/100)%100)+"."+(version%100)+" ";
		}
		if (datetime==null) {
			v+="???";
		} else {
			if (html) {
				v+="<i>( ";
			}
			v+=df.format(new Date((long)(1000.0*datetime)));
			if (html) {
				v+=" )</i>";
			}
		}
		return v;
	}
	
	/**
	 * Return the node name (host name) that this region server is using as its primary contact point.
	 *
	 * @return Short name of the backend LSLR server node in use
	 */
	@Nullable
	public String getAuthNode() {
		return dqs("select authnode from regions where regionid=?",getId());
	}
	
	/**
	 * Convenience method that logs a Server version
	 *
	 * @param st          State
	 * @param version     Version "XX.YY.ZZ"
	 * @param versiondate Preprocessor macro _DATE_
	 * @param versiontime Preprocessor macro _TIME_
	 */
	public void recordServerVersion(@Nonnull final State st,
	                                @Nonnull final String version,
	                                @Nonnull final String versiondate,
	                                @Nonnull final String versiontime,
	                                final int protocol) {
		recordVersion(st,"server",version,versiondate,versiontime,protocol);
	}
	
	/**
	 * Extract the currently known HUD version.
	 *
	 * @param html As HTML?
	 * @return HUD version string
	 */
	@Nonnull
	public String getHUDVersion(final boolean html) {
		final ResultsRow r=dqone("select regionhudversion,regionhuddatetime from regions where regionid=?",getId());
		return formatVersion(r.getIntNullable("regionhudversion"),r.getIntNullable("regionhuddatetime"),html);
	}
	
	/**
	 * Figure out if this region is running needs to update to the latest version of the server/hud package.
	 *
	 * @return True if the region requires an update, false otherwise
	 */
	public boolean needsUpdate() {
		final Integer ourserver=dqi("select regionserverversion from regions where regionid=?",getId());
		final Integer maxserver=dqi("select MAX(regionserverversion) from regions");
		if (ourserver!=null&&maxserver!=null) {
			return maxserver>ourserver;
		}
		return false;
	}
	
	/**
	 * Push a message to this region's server
	 *
	 * @param json JSON Message to send
	 */
	public void sendServer(@Nonnull final JSONObject json) {
		new Transmission(this,json).start();
	}
	
	/**
	 * Return the avatars currently visiting this region
	 *
	 * @return Set of User currently visiting this region.
	 */
	@Nonnull
	public Set<User> getAvatarOpenVisits() {
		final Set<User> users=new HashSet<>();
		final Results results=dq("select avatarid from visits where regionid=? and endtime is null",getId());
		for (final ResultsRow r: results) {
			try {
				users.add(User.get(r.getInt("avatarid")));
			} catch (@Nonnull final Exception ignored) {
			}
		}
		return users;
	}
	
	/**
	 * Get all zones that are present in this region.
	 *
	 * @return Set of Zone objects for this region.
	 */
	@Nonnull
	public Set<Zone> getZones() {
		final Set<Zone> zones=new TreeSet<>();
		for (final ResultsRow r: dq("select distinct zoneid from zoneareas where regionid=?",getId())) {
			zones.add(Zone.get(r.getInt()));
		}
		return zones;
	}
	
	/**
	 * Broadcast the new zoning for this region via the region server
	 */
	public void pushZoning() {
		final JSONObject j=new JSONObject();
		j.put("incommand","broadcast");
		j.put("zoning",ZoneTransport.createZoneTransport(this));
		final Transmission t=new Transmission(this,j);
		t.start();
	}
	
	/**
	 * Count the number of open visits for this region
	 *
	 * @return Number of open visits (current visitors)
	 */
	public int getOpenVisitCount() {
		return dqinn("select count(*) from visits where endtime is null and regionid=?",getId());
	}
	
	/**
	 * Return the online/offline status of this region as a string
	 *
	 * @return String, starts with OFFLINE or STALLED if problematic, otherwise "Online"
	 */
	@Nonnull
	public String getOnlineStatus(final String timezone) {
		final int urllast=getURLLast();
		if (isRetired()) {
			return "Retired";
		}
		if (urllast==0) {
			return "OFFLINE forever?";
		}
		if (getURLNullable()==null||getURLNullable().isEmpty()) {
			return "OFFLINE for "+duration(getUnixTime()-urllast);
		}
		final String authnode=getAuthNode();
		if ((getUnixTime()-urllast)>(15*60)) {
			return "STALLED at "+fromUnixTime(urllast,timezone)+" via server "+authnode;
		}
		return "Online at "+fromUnixTime(urllast,timezone)+" via server "+authnode;
	}
	
	/**
	 * Extract the currently known server version.
	 *
	 * @param html As HTML?
	 * @return Server version string
	 */
	@Nonnull
	public String getServerVersion(final boolean html) {
		final ResultsRow r=
				dqone("select regionserverversion,regionserverdatetime from regions where regionid=?",getId());
		return formatVersion(r.getIntNullable("regionserverversion"),r.getIntNullable("regionserverdatetime"),html);
	}
	
	// ----- Internal Instance -----
	
	public void sendServerSync(@Nonnull final JSONObject json) {
		final Transmission t=new Transmission(this,json);
		//noinspection CallToThreadRun
		t.run();
		if (t.failed()) {
			throw new UserRemoteFailureException("Connection to server failed");
		}
	}
	
	/**
	 * update the global co-ordinates for this region
	 *
	 * @param x Global X
	 * @param y Global Y
	 */
	public void setGlobalCoordinates(final int x,final int y) {
		d("update regions set regionx=?,regiony=? where regionid=?",x,y,getId());
	}
	
	/**
	 * Returns the region's global co-ordinates as a 3d vector String
	 *
	 * @return The region's global co-ordinates as a String vector - "&lt;x,y,0&gt;"
	 */
	@Nonnull
	public String getGlobalCoordinates() {
		final ResultsRow r=dqone("select regionx,regiony from regions where regionid=?",getId());
		final Integer x=r.getIntNullable("regionx");
		final Integer y=r.getIntNullable("regiony");
		if (x==null||y==null) {
			throw new UserRemoteFailureException(
					"Unable to extract "+getNameSafe()+"'s global co-ordinates.  Try '*reboot'ing the region server");
		}
		return "<"+x+","+y+",0>";
	}
	
	public void retire() {
		set("retired",1);
		retiredCache.purge(this);
	}
}
