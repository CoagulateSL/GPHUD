package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Modules.Objects.ObjectTypes.ObjectType;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import java.util.Set;
import java.util.TreeSet;

public class ObjectTypes extends TableRow {
	public ObjectTypes(int id) {
		super(id);
	}

	public static ObjectTypes get(int id) {
		return (ObjectTypes) factoryPut("ObjectTypes", id, new ObjectTypes(id));
	}

	public static ObjectTypes create(State st, String name, JSONObject behaviour) {
		int existing= GPHUD.getDB().dqi(true,"select count(*) from objecttypes where instanceid=? and name like ?",st.getInstance().getId(),name);
		if (existing>0) { throw new UserException("ObjectType "+name+" already exists in instance "+st.getInstance()); }
		GPHUD.getDB().d("insert into objecttypes(instanceid,name,behaviour) values (?,?,?)",st.getInstance().getId(),name,behaviour.toString());
		int newid=GPHUD.getDB().dqi(true,"select id from objecttypes where instanceid=? and name like ?",st.getInstance().getId(),name);
		return get(newid);
	}

	public static String dumpTypes(State st) {
		String r="<table>";
		r+="<tr><th>Name</th><th>Behaviour</th></tr>";
		for (ResultsRow row:GPHUD.getDB().dq("select * from objecttypes where instanceid=?",st.getInstance().getId())) {
			r+="<tr>";
			ObjectTypes ot=get(row.getInt("id"));
			r+="<td><a href=\"/GPHUD/configuration/objects/objecttypes/"+row.getInt("id")+"\">"+row.getString("name")+"</a></td>";
			r+="<td>"+ ObjectType.materialise(st,ot).explainHtml()+"</td>";
			r+="</tr>";
		}
		r+="</table>";
		return r;
	}

	public static Set<String> getObjectTypes(State st) {
		Set<String> set=new TreeSet<>();
		for (ResultsRow row:GPHUD.getDB().dq("select name from objecttypes where instanceid=?",st.getInstance().getId())) {
			set.add(row.getString("name"));
		}
		return set;
	}

	public static DropDownList getDropDownList(State st,String name) {
		DropDownList list=new DropDownList(name);
		for (ResultsRow row:GPHUD.getDB().dq("select name,id from objecttypes where instanceid=?",st.getInstance().getId())) {
			list.add(row.getInt("id").toString(),row.getString("name"));
		}
		return list;
	}

	@Override
	public String getIdField() { return "id"; }

	@Override
	public void validate(State st) throws SystemException {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getInstance()) { throw new SystemException("ObjectTypes / State Instance mismatch"); }
	}

	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}
	public JSONObject getBehaviour() {
		String s=getString("behaviour");
		if (s==null || s.isEmpty()) { return new JSONObject(); }
		return new JSONObject(s);
	}
	public void setBehaviour(JSONObject json) {
		set("behaviour",json.toString());
	}

	@Override
	public String getNameField() { return "name"; }

	@Override
	public String getLinkTarget() { return "/GPHUD/configuration/objects/objecttypes/"+getId(); }

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
		return "objecttypes";
	}

}

