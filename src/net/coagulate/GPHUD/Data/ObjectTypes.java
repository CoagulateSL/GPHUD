package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Modules.Objects.ObjectTypes.ObjectType;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;

public class ObjectTypes extends TableRow {
	public ObjectTypes(int id) {
		super(id);
	}

	@Nonnull
	public static ObjectTypes get(int id) {
		return (ObjectTypes) factoryPut("ObjectTypes", id, new ObjectTypes(id));
	}

	@Nonnull
	public static ObjectTypes create(@Nonnull State st, String name, @Nonnull JSONObject behaviour) {
		int existing= GPHUD.getDB().dqi("select count(*) from objecttypes where instanceid=? and name like ?",st.getInstance().getId(),name);
		if (existing>0) { throw new UserException("ObjectType "+name+" already exists in instance "+st.getInstance()); }
		GPHUD.getDB().d("insert into objecttypes(instanceid,name,behaviour) values (?,?,?)",st.getInstance().getId(),name,behaviour.toString());
		int newid=GPHUD.getDB().dqi("select id from objecttypes where instanceid=? and name like ?",st.getInstance().getId(),name);
		return get(newid);
	}

	@Nonnull
	public static String dumpTypes(@Nonnull State st) {
		StringBuilder r= new StringBuilder("<table>");
		r.append("<tr><th>Name</th><th>Behaviour</th></tr>");
		for (ResultsRow row:GPHUD.getDB().dq("select * from objecttypes where instanceid=?",st.getInstance().getId())) {
			r.append("<tr>");
			ObjectTypes ot=get(row.getIntNullable("id"));
			r.append("<td><a href=\"/GPHUD/configuration/objects/objecttypes/").append(row.getIntNullable("id")).append("\">").append(row.getStringNullable("name")).append("</a></td>");
			r.append("<td>").append(ObjectType.materialise(st, ot).explainHtml()).append("</td>");
			r.append("</tr>");
		}
		r.append("</table>");
		return r.toString();
	}

	@Nonnull
	public static Set<String> getObjectTypes(@Nonnull State st) {
		Set<String> set=new TreeSet<>();
		for (ResultsRow row:GPHUD.getDB().dq("select name from objecttypes where instanceid=?",st.getInstance().getId())) {
			set.add(row.getStringNullable("name"));
		}
		return set;
	}

	@Nonnull
	public static DropDownList getDropDownList(@Nonnull State st, String name) {
		DropDownList list=new DropDownList(name);
		for (ResultsRow row:GPHUD.getDB().dq("select name,id from objecttypes where instanceid=?",st.getInstance().getId())) {
			list.add(row.getIntNullable("id").toString(),row.getStringNullable("name"));
		}
		return list;
	}

	@Nonnull
	@Override
	public String getIdField() { return "id"; }

	@Override
	public void validate(@Nonnull State st) throws SystemException {
		if (validated) { return; }
		validate();
		if (st.getInstance() != getInstance()) { throw new SystemException("ObjectTypes / State Instance mismatch"); }
	}

	@Nullable
	public Instance getInstance() {
		return Instance.get(getIntNullable("instanceid"));
	}
	@Nonnull
	public JSONObject getBehaviour() {
		String s=getString("behaviour");
		if (s==null || s.isEmpty()) { return new JSONObject(); }
		return new JSONObject(s);
	}
	public void setBehaviour(@Nonnull JSONObject json) {
		set("behaviour",json.toString());
	}

	@Nonnull
	@Override
	public String getNameField() { return "name"; }

	@Nonnull
	@Override
	public String getLinkTarget() { return "/GPHUD/configuration/objects/objecttypes/"+getId(); }

	@Override
	protected int getNameCacheTime() { return 600; }

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

	@Nonnull
	@Override
	public String getTableName() {
		return "objecttypes";
	}

}

