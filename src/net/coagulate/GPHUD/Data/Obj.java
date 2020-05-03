package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Objects.ObjectTypes.ObjectType;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Obj extends TableRow {
	public Obj(final int id) {
		super(id);
	}

	// ---------- STATICS ----------
	@Nonnull
	public static Obj get(final int id) {
		return (Obj) factoryPut("Objects",id,new Obj(id));
	}

	@Nonnull
	public static Obj find(final State st,
	                       final String uuid) {
		final int id=db().dqinn("select id from objects where uuid=?",uuid);
		return new Obj(id);
	}

	@Nullable
	public static Obj findOrNull(final State st,
	                             final String uuid) {
		try { return find(st,uuid); } catch (@Nonnull final NoDataException e) { return null; }
	}

	/**
	 * Find the highest registered version of the object driver script
	 *
	 * @return The highest version number of the driver script that objects have connected with
	 */
	public static int getMaxVersion() {
		try {
			return db().dqinn("select max(version) as maxver from objects");
		}
		catch (@Nonnull final NoDataException e) { return 0; }
	}

	/**
	 * Creates a HTML page of objects at this instance...
	 * //TODO
	 *
	 * @param st State
	 *
	 * @return HTML String page thing
	 */
	@Nonnull
	public static String dumpObjects(@Nonnull final State st) {
		final Instance instance=st.getInstance();
		final StringBuilder r=new StringBuilder(
				"<table border=0><tr><th>UUID</th><th>name</th><th>Owner</th><th>Region</th><th>Location</th><th>Version</th><th>Last RX</th><Th>Object Type</th></tr>");
		for (final ResultsRow row: db().dq("select objects.*,UNIX_TIMESTAMP()-lastrx as since from objects,regions where objects.regionid=regions.regionid and regions"+".instanceid=?",
		                                   instance.getId()
		                                  )) {
			final int since=row.getInt("since");
			String bgcol="#dfffdf";
			if (since>16*60) { bgcol="#ffffdf"; }
			if (since>60*60) { bgcol="#ffdfdf"; }
			r.append("<tr bgcolor=").append(bgcol).append(">");
			r.append("<td>").append(row.getStringNullable("uuid")).append("</td>");
			r.append("<td>").append(row.getStringNullable("name")).append("</td>");
			r.append("<td>").append(User.get(row.getInt("owner")).getGPHUDLink()).append("</td>");
			r.append("<td>").append(Region.get(row.getInt("regionid"),true).asHtml(st,true)).append("</td>");
			r.append("<td>").append(row.getStringNullable("location")).append("</td>");
			r.append("<td>").append(row.getIntNullable("version")).append("</td>");
			r.append("<td>").append(UnixTime.duration(since)).append(" ago</td>");
			if (st.hasPermission("Objects.MapObjects")) {
				String objecttype=st.postmap().get(row.getString("uuid"));
				if (!objecttype.isEmpty()) {
					final Integer oldobjecttype=row.getIntNullable("objecttype");
					if (oldobjecttype==null || oldobjecttype!=Integer.parseInt(objecttype)) {
						db().d("update objects set objecttype=? where id=?",objecttype,row.getIntNullable("id"));
						Audit.audit(st,
						            Audit.OPERATOR.AVATAR,
						            null,
						            null,
						            "Set",
						            "ObjectType",
						            "",objecttype,"Set object type for "+row.getStringNullable("name")+" "+row.getStringNullable("uuid")
						           );
						final ObjectType ot=ObjectType.materialise(st,ObjType.get(Integer.parseInt(objecttype)));
						final JSONObject reconfigurepayload=new JSONObject();
						ot.payload(st,reconfigurepayload);
						new Transmission(Obj.get(row.getInt("id")),reconfigurepayload).start();
					}
				}
				else { objecttype=row.getStringNullable("objecttype"); }
				r.append("<td>")
				 .append(ObjType.getDropDownList(st,row.getString("uuid")).submitOnChange().setValue(objecttype).asHtml(st,true))
				 .append("</td>"); // editing too, have fun with that.
			}
			else {
				r.append("<td>").append(row.getStringNullable("objecttype")).append("</td>");
			}
			if (st.hasPermission("Objects.RebootObjects")) {
				r.append("<td><button type=Submit name=reboot value=\"").append(row.getStringNullable("uuid")).append("\">Reboot</button></td>");
			}
			if (st.hasPermission("Objects.ShutdownObjects")) {
				if (row.getString("uuid").equals(st.postmap().get("shutdown"))) {
					r.append("<td><button type=Submit name=reallyshutdown value=\"")
					 .append(row.getStringNullable("uuid"))
					 .append("\">CONFIRM SHUTDOWN - THE OBJECT OWNER MUST REBOOT IT TO RESUME SERVICE</button></td>");
				}
				else {
					r.append("<td><button type=Submit name=shutdown value=\"").append(row.getStringNullable("uuid")).append("\">Shutdown</button></td>");
				}
			}
			r.append("</tr>");
		}
		r.append("</table><br><i>(Objects are expected to check in once every 15 minutes, though if a region is down this may not happen.  Connections are purged after 24 "+"hours inactivity, the object type configuration is not, and can be relinked to a new connection.)</i>");
		return r.toString();
	}

	/**
	 * Connect an object
	 *
	 * @param st       State
	 * @param uuid     The Object's UUID
	 * @param name     The Object's Name
	 * @param region   The Object's Region
	 * @param owner    The Object's Owner
	 * @param location The Object's Location
	 * @param url      The Object's callback URL
	 * @param version  The Object's driver script version
	 *
	 * @return The Objects connector for this Object
	 */
	@Nonnull
	public static Obj connect(@Nonnull final State st,
	                          @Nonnull final String uuid,
	                          @Nonnull final String name,
	                          @Nonnull final Region region,
	                          @Nonnull final User owner,
	                          @Nonnull final String location,
	                          @Nonnull final String url,
	                          final int version) {
		Obj object=findOrNull(st,uuid);
		if (object==null) {
			db().d("insert into objects(uuid,name,regionid,owner,location,lastrx,url,version) values(?,?,?,?,?,?,?,?)",
			       uuid,
			       name,
			       region.getId(),
			       owner.getId(),
			       location,
			       UnixTime.getUnixTime(),
			       url,
			       version
			      );
			object=findOrNull(st,uuid);
			if (object==null) {
				throw new SystemConsistencyException("Object not found for uuid "+uuid+" after creating it");
			}
		}
		else {
			db().d("update objects set name=?,regionid=?,owner=?,location=?,lastrx=?,url=?,version=? where id=?",
			       name,
			       region.getId(),
			       owner.getId(),
			       location,
			       UnixTime.getUnixTime(),
			       url,
			       version,
			       object.getId()
			      );
		}
		return object;
	}

	/**
	 * Returns number of object connections to be purged
	 */
	public static int getPurgeInactiveCount() {
		return db().dqinn("select count(*) from objects where lastrx<(UNIX_TIMESTAMP()-(60*60*24))");
	}

	/**
	 * Purges connections that have been idle over 24 hours
	 */
	public static void purgeInactive() {
		db().d("delete from objects where lastrx<(UNIX_TIMESTAMP()-(60*60*24))");
	}

	public static Table statusDump(final State st) {
		final Table t=new Table().border();
		t.header("UUID");
		t.header("Name");
		t.header("Region");
		t.header("Owner");
		t.header("Location");
		t.header("Last RX");
		t.header("ObjectType");
		t.header("URL");
		t.header("Version");
		t.header("Servicing Server");
		for (final ResultsRow row: db().dq("select objects.* from objects inner join regions on objects.regionid=regions.regionid where regions.instanceid=?",
		                                   st.getInstance().getId()
		                                  )) {
			t.openRow();
			t.add(row.getString("uuid"));
			t.add(row.getString("name"));
			t.add(Region.get(row.getInt("regionid"),true).getName()+"[#"+row.getInt("regionid")+"]");
			t.add(User.get(row.getInt("owner")).getName()+"[#"+row.getInt("regionid")+"]");
			t.add(row.getString("location"));
			t.add(UnixTime.fromUnixTime(row.getIntNullable("lastrx"),st.getAvatar().getTimeZone()));
			final Integer objecttype=row.getIntNullable("objecttype");
			t.add(objecttype==null?"":ObjType.get(objecttype).getName()+"[#"+objecttype+"]");
			t.add(row.getStringNullable("url")==null?"":"Present");
			t.add(row.getIntNullable("version"));
			t.add(row.getStringNullable("authnode"));
		}
		return t;
	}

	// ---------- INSTANCE ----------

	/**
	 * Get the Object Type for this object
	 *
	 * @return The ObjectType, or null if the object is not yet bound to a type
	 */
	@Nullable
	public ObjType getObjectType() {
		final Integer otid=getIntNullable("objecttype");
		if (otid==null) { return null; }
		return ObjType.get(otid);
	}

	@Nonnull
	@Override
	public String getIdColumn() { return "id"; }

	@Override
	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Object / State Instance mismatch");
		}
	}

	@Nonnull
	@Override
	public String getNameField() { return "name"; }

	@Nonnull
	@Override
	public String getLinkTarget() { return "/GPHUD/configuration/objects/object/"+getId(); }

	@Nonnull
	public String toString() { return "Object#"+getId()+"='"+getName()+"'@"+getRegion()+"/"+getLocation();}

	@Nullable
	@Override
	public String getKVTable() {
		return null;
	}

	@Nullable
	@Override
	public String getKVIdField() {
		return null;
	}

	@Override
	protected int getNameCacheTime() { return 600; }

	/**
	 * Get the region associated with this object
	 *
	 * @return The region
	 */
	@Nonnull
	public Region getRegion() { return Region.get(getInt("regionid"),true); }

	/**
	 * Get the location associated with this object
	 *
	 * @return The string location of this object
	 */
	@Nonnull
	public String getLocation() { return getString("location"); }

	/**
	 * Get this object's instance
	 *
	 * @return The Instance
	 */
	@Nonnull
	public Instance getInstance() {
		return getRegion().getInstance();
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "objects";
	}

	/**
	 * Gets the URL associated with this Object
	 *
	 * @return The URL, which may be null if not connected.
	 */
	@Nullable
	public String getURL() {
		return getStringNullable("url");
	}

	/**
	 * Update the lastrx timer for this object
	 */
	public void updateRX() {
		Integer lastrx=getIntNullable("lastrx");
		if (lastrx==null) { lastrx=0; }
		final int diff=UnixTime.getUnixTime()-lastrx;
		if (diff>60) { db().d("update objects set lastrx=?,authnode=? where id=?",UnixTime.getUnixTime(),Interface.getNode(),getId()); }
	}
}
