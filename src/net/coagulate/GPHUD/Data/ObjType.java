package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemLookupFailureException;
import net.coagulate.Core.Exceptions.User.UserConfigurationException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Objects.ObjectTypes.ObjectType;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.CacheConfig;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;

public class ObjType extends TableRow {
	public ObjType(final int id) {
		super(id);
	}
	
	/**
	 * Create a new object type
	 *
	 * @param st        State
	 * @param name      Name of object type
	 * @param behaviour The JSON object describing the object's behaviour (See Objects module)
	 * @return a new ObjectTypes
	 *
	 * @throws UserInputDuplicateValueException if the object type already exists
	 */
	@Nonnull
	public static ObjType create(@Nonnull final State st,
	                             @Nonnull final String name,
	                             @Nonnull final JSONObject behaviour) {
		final int existing=db().dqiNotNull("select count(*) from objecttypes where instanceid=? and name like ?",
		                                   st.getInstance().getId(),
		                                   name);
		if (existing>0) {
			throw new UserInputDuplicateValueException(
					"ObjectType "+name+" already exists in instance "+st.getInstance());
		}
		db().d("insert into objecttypes(instanceid,name,behaviour) values (?,?,?)",
		       st.getInstance().getId(),
		       name,
		       behaviour.toString());
		final int newid=db().dqiNotNull("select id from objecttypes where instanceid=? and name like ?",
		                                st.getInstance().getId(),
		                                name);
		return get(newid);
	}
	
	// ---------- STATICS ----------
	@Nonnull
	public static ObjType get(final int id) {
		return (ObjType)factoryPut("ObjectTypes",id,ObjType::new);
	}
	
	/**
	 * A horrible bodge that creates a web page of object types.
	 * FIXME
	 *
	 * @param st The state
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
			r.append("<td>");
			try {
				r.append(ObjectType.materialise(st,ot).explainHtml());
			} catch (final SystemLookupFailureException e) {
				r.append("<i>NoType</i>");
			}
			r.append("</td>");
			if (st.hasPermission("Objects.ObjectTypes")) {
				r.append("<td>");
				r.append(new Form(st,
				                  true,
				                  "/GPHUD/Configuration/Objects/DeleteObjectType",
				                  "Delete",
				                  "name",
				                  row.getStringNullable("name")).asHtml(st,true));
				r.append("</td>");
			}
			r.append("</tr>");
		}
		r.append("</table>");
		return r.toString();
	}
	
	/**
	 * Gets a set of all the object type names at this instance.
	 *
	 * @param st State
	 * @return A Set of Strings of objecttype names
	 */
	@Nonnull
	public static Set<String> getObjectTypes(@Nonnull final State st) {
		final Set<String> set=new TreeSet<>();
		for (final ResultsRow row: db().dq("select name from objecttypes where instanceid=?",
		                                   st.getInstance().getId())) {
			set.add(row.getStringNullable("name"));
		}
		return set;
	}
	
	/**
	 * Creates a DropDownList of all the object types available at this instance.
	 *
	 * @param st   State
	 * @param name Name of the HTML DropDownList component
	 * @return a DropDownList containing all the object types that will put the ID number in the HTML form
	 */
	@Nonnull
	public static DropDownList getDropDownList(@Nonnull final State st,@Nonnull final String name) {
		final DropDownList list=new DropDownList(name);
		for (final ResultsRow row: db().dq("select name,id from objecttypes where instanceid=?",
		                                   st.getInstance().getId())) {
			list.add(String.valueOf(row.getInt("id")),row.getStringNullable("name"));
		}
		return list;
	}
	
	@Nullable
	public static ObjType getNullable(final State state,final String objecttype) {
		try {
			return get(GPHUD.getDB()
			                .dqiNotNull("select id from objecttypes where instanceid=? and name like ?",
			                            state.getInstance().getId(),
			                            objecttype));
		} catch (final NoDataException e) {
			return null;
		}
	}
	public static ObjType get(final State state,final String objecttype) {
		try {
			return get(GPHUD.getDB()
			                .dqiNotNull("select id from objecttypes where instanceid=? and name like ?",
			                            state.getInstance().getId(),
			                            objecttype));
		} catch (final NoDataException e) {
			throw new UserConfigurationException("Object type "+objecttype+" does not exist",e,true);
		}
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String getIdColumn() {
		return "id";
	}
	
	@Override
	public void validate(@Nonnull final State st) {
		if (validated) {
			return;
		}
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("ObjectTypes / State Instance mismatch");
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
		return "/GPHUD/configuration/objects/objecttypes/"+getId();
	}
	
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
		return behaviourCache.get(getId(),()->{
			String s=null;
			try {
				s=getStringNullable("behaviour");
			} catch (final NoDataException ignore) {
			}
			if (s==null||s.isEmpty()) {
				return new JSONObject();
			}
			return new JSONObject(s);
		});
	}
	
	private static final Cache<Integer,JSONObject> behaviourCache=
			Cache.getCache("GPHUD/ObjectTypeBehaviour",CacheConfig.PERMANENT_CONFIG);
	
	/**
	 * Set this objecttype's behaviour JSON
	 *
	 * @param json The new ObjectType's behaviour JSON (out of scope, see Objects module)
	 */
	public void setBehaviour(@Nonnull final JSONObject json) {
		set("behaviour",json.toString());
		behaviourCache.set(getId(),json);
	}
	
	@Nonnull
	@Override
	public String getTableName() {
		return "objecttypes";
	}
	
	public void delete() {
		behaviourCache.purge(getId());
		d("delete from objecttypes where id=?",getId());
	}
}

