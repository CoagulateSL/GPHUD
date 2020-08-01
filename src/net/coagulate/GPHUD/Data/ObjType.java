package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Modules.Objects.ObjectTypes.ObjectType;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;

public class ObjType extends TableRow {
	public ObjType(final int id) {
		super(id);
	}

	// ---------- STATICS ----------
	@Nonnull
	public static ObjType get(final int id) {
		return (ObjType) factoryPut("ObjectTypes",id,new ObjType(id));
	}

	/**
	 * Create a new object type
	 *
	 * @param st        State
	 * @param name      Name of object type
	 * @param behaviour The JSON object describing the object's behaviour (See Objects module)
	 *
	 * @return a new ObjectTypes
	 *
	 * @throws UserInputDuplicateValueException if the object type already exists
	 */
	@Nonnull
	public static ObjType create(@Nonnull final State st,
	                             @Nonnull final String name,
	                             @Nonnull final JSONObject behaviour) {
		final int existing=db().dqinn("select count(*) from objecttypes where instanceid=? and name like ?",st.getInstance().getId(),name);
		if (existing>0) {
			throw new UserInputDuplicateValueException("ObjectType "+name+" already exists in instance "+st.getInstance());
		}
		db().d("insert into objecttypes(instanceid,name,behaviour) values (?,?,?)",st.getInstance().getId(),name,behaviour.toString());
		final int newid=db().dqinn("select id from objecttypes where instanceid=? and name like ?",st.getInstance().getId(),name);
		return get(newid);
	}

	/**
	 * A horrible bodge that creates a web page of object types.
	 * FIXME
	 *
	 * @param st The state
	 *
	 * @return A HTML string...
	 */
	@Nonnull
	public static String dumpTypes(@Nonnull final State st) {
		final StringBuilder r=new StringBuilder("<table>");
		r.append("<tr><th>Name</th><th>Behaviour</th></tr>");
		for (final ResultsRow row: db().dq("select * from objecttypes where instanceid=?",st.getInstance().getId())) {
			r.append("<tr>");
			final ObjType ot=get(row.getInt("id"));
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

	/**
	 * Gets a set of all the object type names at this instance.
	 *
	 * @param st State
	 *
	 * @return A Set of Strings of objecttype names
	 */
	@Nonnull
	public static Set<String> getObjectTypes(@Nonnull final State st) {
		final Set<String> set=new TreeSet<>();
		for (final ResultsRow row: db().dq("select name from objecttypes where instanceid=?",st.getInstance().getId())) {
			set.add(row.getStringNullable("name"));
		}
		return set;
	}

	/**
	 * Creates a DropDownList of all the object types available at this instance.
	 *
	 * @param st   State
	 * @param name Name of the HTML DropDownList component
	 *
	 * @return a DropDownList containing all the object types that will put the ID number in the HTML form
	 */
	@Nonnull
	public static DropDownList getDropDownList(@Nonnull final State st,
	                                           @Nonnull final String name) {
		final DropDownList list=new DropDownList(name);
		for (final ResultsRow row: db().dq("select name,id from objecttypes where instanceid=?",st.getInstance().getId())) {
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
	// ----- Internal Instance -----

	/**
	 * Returns the instance associated with this objecttype.
	 *
	 * @return The Instance
	 */
	@Nonnull
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}

	/**
	 * Gets the behaviour JSON for this objecttype.
	 *
	 * @return The ObjectType's behaviour JSON (out of scope, see Objects module)
	 */
	@Nonnull
	public JSONObject getBehaviour() {
		final String s=getStringNullable("behaviour");
		if (s==null || s.isEmpty()) { return new JSONObject(); }
		return new JSONObject(s);
	}

	/**
	 * Set this objecttype's behaviour JSON
	 *
	 * @param json The new ObjectType's behaviour JSON (out of scope, see Objects module)
	 */
	public void setBehaviour(@Nonnull final JSONObject json) {
		set("behaviour",json.toString());
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "objecttypes";
	}
	@Override
	protected int getNameCacheTime() { return 60*60; }

}

