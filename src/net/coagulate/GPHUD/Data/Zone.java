package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
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

	protected Zone(int id) { super(id); }

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return A zone representation
	 */
	@Nonnull
	public static Zone get(int id) {
		return (Zone) factoryPut("Zone", id, new Zone(id));
	}

	/**
	 * Create a new zone.
	 *
	 * @param instance Instance the zone binds to
	 * @param name     Name of the zone
	 * @throws UserException If the zone name is in use
	 */
	public static void create(@Nonnull Instance instance, String name) throws UserException {
		if (GPHUD.getDB().dqi(true, "select count(*) from zones where instanceid=? and name like ?", instance.getId(), name) > 0) {
			throw new UserException("Zone name already in use");
		}
		GPHUD.getDB().d("insert into zones(instanceid,name) values(?,?)", instance.getId(), name);
	}

	/**
	 * Find a zone by name.
	 *
	 * @param instance Instance to search
	 * @param name     Name of the zone
	 * @return Zone object, or null
	 */
	@Nullable
	public static Zone find(@Nonnull Instance instance, String name) {
		Integer zoneid = GPHUD.getDB().dqi(false, "select zoneid from zones where name like ? and instanceid=?", name, instance.getId());
		if (zoneid == null) { return null; }
		return get(zoneid);
	}

	static void wipeKV(@Nonnull Instance instance, String key) {
		String kvtable = "zonekvstore";
		String maintable = "zones";
		String idcolumn = "zoneid";
		GPHUD.getDB().d("delete from " + kvtable + " using " + kvtable + "," + maintable + " where " + kvtable + ".k like ? and " + kvtable + "." + idcolumn + "=" + maintable + "." + idcolumn + " and " + maintable + ".instanceid=?", key, instance.getId());
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
		Set<ZoneArea> areas = new TreeSet<>();
		for (ResultsRow r : dq("select zoneareaid from zoneareas where zoneid=?", getId())) {
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
		Set<ZoneArea> areas = getZoneAreas();
		StringBuilder s = new StringBuilder();
		for (ZoneArea a : areas) {
			String[] vectors = a.getVectors();
			if (s.length() > 0) { s.append("|"); }
			s = new StringBuilder(getName() + "|" + vectors[0] + "|" + vectors[1]);
		}
		return s.toString();
	}

	/**
	 * Return the instance associated with this zone
	 *
	 * @return Instance object
	 */
	@Nullable
	public Instance getInstance() {
		Integer id = getInt("instanceid");
		if (id == null) {
			throw new SystemException("Zone " + getName() + " #" + getId() + " is not associated with an instance?");
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
	public void addArea(@Nonnull Region region, String cornerOne, String cornerTwo) {
		int[] c1 = ZoneArea.parseVector(cornerOne);
		int[] c2 = ZoneArea.parseVector(cornerTwo);
		d("insert into zoneareas(zoneid,regionid,x1,y1,z1,x2,y2,z2) values(?,?,?,?,?,?,?,?)", getId(), region.getId(), c1[0], c1[1], c1[2], c2[0], c2[1], c2[2]);
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
	public void broadcastMessage(String message) {
		JSONObject json = new JSONObject();
		json.put("incommand", "broadcast");
		json.put("zonemessage", message);
		json.put("zone", getName());
		GPHUD.getLogger().info("Sending broadcast to zone " + getName() + " in instance " + getInstance().getName() + " - " + message);
		getInstance().sendServers(json);
	}

	public void validate(@Nonnull State st) throws SystemException {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getInstance()) { throw new SystemException("Zone / State Instance mismatch"); }
	}

	@Override
	protected int getNameCacheTime() { return 60 * 60; } // this name doesn't change, cache 1 hour

}

