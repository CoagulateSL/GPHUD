package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UnixTime;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Objects.ObjectTypes.ObjectType;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

public class Objects extends TableRow {
	public Objects(int id) {
		super(id);
	}

	public static Objects get(int id) {
		return (Objects) factoryPut("Objects", id, new Objects(id));
	}

	public static Objects find(State st, String uuid) {
		Integer id=GPHUD.getDB().dqi(true,"select id from objects where uuid=?",uuid);
		return new Objects(id);
	}
	public static Objects findOrNull(State st, String uuid) {
		Integer id=GPHUD.getDB().dqi(false,"select id from objects where uuid=?",uuid);
		if (id==null) { return null; }
		return new Objects(id);
	}

	public static int getMaxVersion() {
		Integer version= GPHUD.getDB().dqi(false, "select max(version) as maxver from objects");
		if (version==null) { return 0; }
		return version;
	}

	public ObjectTypes getObjectType() {
		Integer otid=getInt("objecttype");
		if (otid==null) { return null; }
		return ObjectTypes.get(otid);
	}

	public static String dumpObjects(State st) {
		Instance instance=st.getInstance();
		String r="<table border=0><tr><th>UUID</th><th>name</th><th>Owner</th><th>Region</th><th>Location</th><th>Version</th><th>Last RX</th><Th>Object Type</th></tr>";
		for (ResultsRow row:GPHUD.getDB().dq("select objects.*,UNIX_TIMESTAMP()-lastrx as since from objects,regions where objects.regionid=regions.regionid and regions.instanceid=?",instance.getId())) {
			int since=row.getInt("since");
			String bgcol="#dfffdf";
			if (since>16*60) { bgcol="#ffffdf"; }
			if (since>60*60) { bgcol="#ffdfdf"; }
			r+="<tr bgcolor="+bgcol+">";
			r+="<td>"+row.getString("uuid")+"</td>";
			r+="<td>"+row.getString("name")+"</td>";
			r+="<td>"+User.get(row.getInt("owner")).getGPHUDLink()+"</td>";
			r+="<td>"+Region.get(row.getInt("regionid"),true).asHtml(st,true)+"</td>";
			r+="<td>"+row.getString("location")+"</td>";
			r+="<td>"+row.getInt("version")+"</td>";
			r+="<td>"+UnixTime.duration(since)+" ago</td>";
			if (st.hasPermission("Objects.MapObjects")) {
				String objecttype = st.postmap.get(row.getString("uuid"));
				if (!objecttype.isEmpty()) {
					GPHUD.getDB().d("update objects set objecttype=? where id=?", objecttype, row.getInt("id"));
					Audit.audit(st, Audit.OPERATOR.AVATAR,null,null,"Set","ObjectType","",objecttype,"Set object type for "+row.getString("name")+" "+row.getString("uuid"));
					ObjectType ot= ObjectType.materialise(st,ObjectTypes.get(Integer.parseInt(objecttype)));
					JSONObject reconfigurepayload=new JSONObject();
					ot.payload(st,reconfigurepayload);
					new Transmission(Objects.get(row.getInt("id")),reconfigurepayload).start();
				} else { objecttype=row.getString("objecttype"); }
				r += "<td>" + ObjectTypes.getDropDownList(st, row.getString("uuid")).submitOnChange().setValue(objecttype).asHtml(st, true) + "</td>"; // editing too, have fun with that.
			} else {
				r+="<td>"+row.getString("objecttype")+"</td>";
			}
			if (st.hasPermission("Objects.RebootObjects")) {
				r+="<td><button type=Submit name=reboot value=\""+row.getString("uuid")+"\">Reboot</button></td>";
			}
			if (st.hasPermission("Objects.ShutdownObjects")) {
				if (row.getString("uuid").equals(st.postmap.get("shutdown"))) {
					r += "<td><button type=Submit name=reallyshutdown value=\"" + row.getString("uuid") + "\">CONFIRM SHUTDOWN - THE OBJECT OWNER MUST REBOOT IT TO RESUME SERVICE</button></td>";
				} else {
					r+="<td><button type=Submit name=shutdown value=\""+row.getString("uuid")+"\">Shutdown</button></td>";
				}
			}
			r+="</tr>";
		}
		r+="</table><br><i>(Objects are expected to check in once every 15 minutes, though if a region is down this may not happen.  Connections are purged after 24 hours inactivity, the object type configuration is not, and can be relinked to a new connection.)</i>";
		return r;
	}

	@Override
	public String getIdField() { return "id"; }

	@Override
	public void validate(State st) throws SystemException {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getInstance()) { throw new SystemException("Object / State Instance mismatch"); }
	}

	public Region getRegion() { return Region.get(getInt("regionid"),true); }

	public String getLocation() { return getString("location"); }
	public Instance getInstance() {
		return getRegion().getInstance();
	}

	@Override
	public String getNameField() { return "name"; }

	@Override
	public String getLinkTarget() { return "/GPHUD/configuration/objects/object/"+getId(); }

	@Override
	protected int getNameCacheTime() { return 600; }

	@Override
	public String getKVTable() {
		return null;
	}

	@Override
	public String getKVIdField() {
		return null;
	}

	@Override
	public String getTableName() {
		return "objects";
	}

	public static Objects connect(State st, String uuid, String name, Region region, User owner, String location,String url,int version) {
		Objects object=findOrNull(st,uuid);
		if (object==null) {
			GPHUD.getDB().d("insert into objects(uuid,name,regionid,owner,location,lastrx,url,version) values(?,?,?,?,?,?,?,?)",uuid,name,region.getId(),owner.getId(),location, UnixTime.getUnixTime(),url,version);
			object=findOrNull(st,uuid);
			if (object==null) { throw new SystemException("Object not found for uuid "+uuid+" after creating it"); }
		} else {
			GPHUD.getDB().d("update objects set name=?,regionid=?,owner=?,location=?,lastrx=?,url=?,version=? where id=?",name,region.getId(),owner.getId(),location,UnixTime.getUnixTime(),url,version,object.getId());
		}
		return object;
	}

	public String toString() { return "Object#"+getId()+"='"+getName()+"'@"+getRegion().toString()+"/"+getLocation();}

	public String getURL() {
		return getString("url");
	}
}
