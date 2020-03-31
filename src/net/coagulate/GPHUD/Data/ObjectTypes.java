package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
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
	public ObjectTypes(final int id) {
		super(id);
	}

	// ---------- STATICS ----------
	@Nonnull
	public static ObjectTypes get(final int id) {
		return (ObjectTypes) factoryPut("ObjectTypes",id,new ObjectTypes(id));
	}

	@Nonnull
	public static ObjectTypes create(@Nonnull final State st,
	                                 final String name,
	                                 @Nonnull final JSONObject behaviour) {
		final int existing=GPHUD.getDB().dqinn("select count(*) from objecttypes where instanceid=? and name like ?",st.getInstance().getId(),name);
		if (existing>0) {
			throw new UserInputDuplicateValueException("ObjectType "+name+" already exists in instance "+st.getInstance());
		}
		GPHUD.getDB().d("insert into objecttypes(instanceid,name,behaviour) values (?,?,?)",st.getInstance().getId(),name,behaviour.toString());
		final int newid=GPHUD.getDB().dqinn("select id from objecttypes where instanceid=? and name like ?",st.getInstance().getId(),name);
		return get(newid);
	}

	@Nonnull
	public static String dumpTypes(@Nonnull final State st) {
		final StringBuilder r=new StringBuilder("<table>");
		r.append("<tr><th>Name</th><th>Behaviour</th></tr>");
		for (final ResultsRow row: GPHUD.getDB().dq("select * from objecttypes where instanceid=?",st.getInstance().getId())) {
			r.append("<tr>");
			final ObjectTypes ot=get(row.getInt("id"));
			r.append("<td><a href=\"/GPHUD/configuration/objects/objecttypes/")
			 .append(row.getIntNullable("id"))
			 .append("\">")
			 .append(row.getStringNullable("name"))
			 .append("</a></td>");
			r.append("<td>").append(ObjectType.materialise(st,ot).explainHtml()).append("</td>");
			r.append("</tr>");
		}
		r.append("</table>");
		return r.toString();
	}

	@Nonnull
	public static Set<String> getObjectTypes(@Nonnull final State st) {
		final Set<String> set=new TreeSet<>();
		for (final ResultsRow row: GPHUD.getDB().dq("select name from objecttypes where instanceid=?",st.getInstance().getId())) {
			set.add(row.getStringNullable("name"));
		}
		return set;
	}

	@Nonnull
	public static DropDownList getDropDownList(@Nonnull final State st,
	                                           final String name) {
		final DropDownList list=new DropDownList(name);
		for (final ResultsRow row: GPHUD.getDB().dq("select name,id from objecttypes where instanceid=?",st.getInstance().getId())) {
			list.add(row.getInt("id")+"",row.getStringNullable("name"));
		}
		return list;
	}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String getIdColumn() { return "id"; }

	@Override
	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("ObjectTypes / State Instance mismatch");
		}
	}

	@Nonnull
	@Override
	public String getNameField() { return "name"; }

	@Nonnull
	@Override
	public String getLinkTarget() { return "/GPHUD/configuration/objects/objecttypes/"+getId(); }

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

	@Nullable
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}

	@Nonnull
	public JSONObject getBehaviour() {
		final String s=getStringNullable("behaviour");
		if (s==null || s.isEmpty()) { return new JSONObject(); }
		return new JSONObject(s);
	}

	public void setBehaviour(@Nonnull final JSONObject json) {
		set("behaviour",json.toString());
	}

	// ----- Internal Instance -----

	@Nonnull
	@Override
	public String getTableName() {
		return "objecttypes";
	}

	@Override
	protected int getNameCacheTime() { return 600; }

}

