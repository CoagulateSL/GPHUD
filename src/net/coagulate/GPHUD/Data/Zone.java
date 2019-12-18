package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;

/**
 * A 'zone' - an area of a region.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Zone extends TableRow {

	protected Zone(final int id) { super(id); }

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 *
	 * @return A zone representation
	 */
	@Nonnull
	public static Zone get(final int id) {
		return (Zone) factoryPut("Zone",id,new Zone(id));
	}

	/**
	 * Create a new zone.
	 *
	 * @param instance Instance the zone binds to
	 * @param name     Name of the zone
	 *
	 * @throws UserException If the zone name is in use
	 */
	public static void create(@Nonnull final Instance instance,
	                          final String name)
	{
		if (GPHUD.getDB()
		         .dqinn("select count(*) from zones where instanceid=? and name like ?",instance.getId(),name)>0)
		{
			throw new UserInputDuplicateValueException("Zone name already in use");
		}
		GPHUD.getDB().d("insert into zones(instanceid,name) values(?,?)",instance.getId(),name);
	}

	/**
	 * Find a zone by name.
	 *
	 * @param instance Instance to search
	 * @param name     Name of the zone
	 *
	 * @return Zone object, or null
	 */
	@Nullable
	public static Zone find(@Nonnull final Instance instance,
	                        @Nonnull final String name)
	{
		try {
			final int zoneid=GPHUD.getDB()
			                      .dqinn("select zoneid from zones where name like ? and instanceid=?",
			                             name,
			                             instance.getId()
			                            );
			return get(zoneid);
		} catch (@Nonnull final NoDataException e) { return null; }
	}

	static void wipeKV(@Nonnull final Instance instance,
	                   final String key)
	{
		final String kvtable="zonekvstore";
		final String maintable="zones";
		final String idcolumn="zoneid";
		GPHUD.getDB()
		     .d("delete from "+kvtable+" using "+kvtable+","+maintable+" where "+kvtable+".k like ? and "+kvtable+"."+idcolumn+"="+maintable+"."+idcolumn+" and "+maintable+".instanceid=?",
		        key,
		        instance.getId()
		       );
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "zones";
	}

	@Nonnull
	@Override
	public String getIdField() {
		return "zoneid";
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}

	@Nonnull
	@Override
	public String getLinkTarget() {
		return "/configuration/zoning";
	}

	/**
	 * Get the defined areas for this zone.
	 *
	 * @return Set of ZoneAreas
	 */
	@Nonnull
	public Set<ZoneArea> getZoneAreas() {
		final Set<ZoneArea> areas=new TreeSet<>();
		for (final ResultsRow r: dq("select zoneareaid from zoneareas where zoneid=?",getId())) {
			areas.add(ZoneArea.get(r.getInt()));
		}
		return areas;
	}

	/**
	 * Format the zone and areas for the HUD.
	 * Uses an odd format, | delimited data, this is a normal for compact SL list processing
	 *
	 * @return String for the HUD to store zone data using
	 */
	@Nonnull
	public String getTransportFormat() {
		final Set<ZoneArea> areas=getZoneAreas();
		StringBuilder s=new StringBuilder();
		for (final ZoneArea a: areas) {
			final String[] vectors=a.getVectors();
			if (vectors!=null) {
				if (s.length()>0) { s.append("|"); }
				s=new StringBuilder(getName()+"|"+vectors[0]+"|"+vectors[1]);
			}
		}
		return s.toString();
	}

	/**
	 * Return the instance associated with this zone
	 *
	 * @return Instance object
	 */
	@Nonnull
	public Instance getInstance() {
		final Integer id=getIntNullable("instanceid");
		if (id==null) {
			throw new SystemConsistencyException("Zone "+getName()+" #"+getId()+" is not associated with an instance?");
		}
		return Instance.get(id);
	}

	/**
	 * Add an area to a zone.
	 *
	 * @param region    Region for the area
	 * @param cornerOne Co-ordinate 1 as x,y,z string
	 * @param cornerTwo Co-ordinate 2 as x,y,z string
	 */
	public void addArea(@Nonnull final Region region,
	                    final String cornerOne,
	                    final String cornerTwo)
	{
		final int[] c1=ZoneArea.parseVector(cornerOne);
		final int[] c2=ZoneArea.parseVector(cornerTwo);
		d("insert into zoneareas(zoneid,regionid,x1,y1,z1,x2,y2,z2) values(?,?,?,?,?,?,?,?)",
		  getId(),
		  region.getId(),
		  c1[0],
		  c1[1],
		  c1[2],
		  c2[0],
		  c2[1],
		  c2[2]
		 );
	}

	@Nonnull
	@Override
	public String getKVTable() {
		return "zonekvstore";
	}

	@Nonnull
	@Override
	public String getKVIdField() {
		return "zoneid";
	}

	/**
	 * Broadcast a message to all users in this zone.
	 *
	 * @param message Zonemessage to send to the zone (?)
	 */
	public void broadcastMessage(final String message) {
		final JSONObject json=new JSONObject();
		json.put("incommand","broadcast");
		json.put("zonemessage",message);
		json.put("zone",getName());
		GPHUD.getLogger().info("Sending broadcast to zone "+getName()+" in instance "+getInstance().getName()+" - "+message);
		getInstance().sendServers(json);
	}

	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
		if (st.getInstance()!=getInstance()) { throw new SystemConsistencyException("Zone / State Instance mismatch"); }
	}

	@Override
	protected int getNameCacheTime() { return 60*60; } // this name doesn't change, cache 1 hour

}

