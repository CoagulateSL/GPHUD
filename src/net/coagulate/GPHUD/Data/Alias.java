package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Alias entry.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Alias extends TableRow {

	protected Alias(final int id) { super(id); }

	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 *
	 * @return An Avatar representation
	 */
	@Nonnull
	public static Alias get(final int id) { return (Alias) factoryPut("Alias",id,new Alias(id)); }

	/**
	 * Returns a map of aliases for this state.
	 *
	 * @param st State
	 *
	 * @return Map of Alias Name to Alias objects
	 */
	@Nonnull
	public static Map<String,Alias> getAliasMap(@Nonnull final State st) {
		final Map<String,Alias> aliases=new TreeMap<>();
		for (final ResultsRow r: GPHUD.getDB()
		                              .dq("select name,aliasid from aliases where instanceid=?",
		                                  st.getInstance().getId()
		                                 )) {
			aliases.put(r.getStringNullable("name"),get(r.getInt("aliasid")));
		}
		return aliases;
	}

	/**
	 * Get aliased command templates for this state
	 *
	 * @param st State
	 *
	 * @return Map of Name to Template (JSON) mappings
	 */
	@Nonnull
	public static Map<String,JSONObject> getTemplates(@Nonnull final State st) {
		final Map<String,JSONObject> aliases=new TreeMap<>();
		for (final ResultsRow r: GPHUD.getDB()
		                              .dq("select name,template from aliases where instanceid=?",
		                                  st.getInstance().getId()
		                                 )) {
			aliases.put(r.getStringNullable("name"),new JSONObject(r.getStringNullable("template")));
		}
		return aliases;
	}

	@Nullable
	public static Alias getAlias(@Nonnull final State st,
	                             final String name)
	{
		try {
			final int id=GPHUD.getDB()
			                  .dqinn("select aliasid from aliases where instanceid=? and name like ?",
			                         st.getInstance().getId(),
			                         name
			                        );
			return get(id);
		} catch (@Nonnull final NoDataException e) { return null; }
	}

	@Nonnull
	public static Alias create(@Nonnull final State st,
	                           @Nonnull final String name,
	                           @Nonnull final JSONObject template)
	{
		if (getAlias(st,name)!=null) { throw new UserInputDuplicateValueException("Alias "+name+" already exists"); }
		if (name.matches(".*[^A-Za-z0-9-=_,].*")) {
			throw new UserInputValidationParseException(
					"Aliases must not contain spaces, and mostly only allow A-Z a-z 0-9 - + _ ,");
		}
		GPHUD.getDB()
		     .d("insert into aliases(instanceid,name,template) values(?,?,?)",
		        st.getInstance().getId(),
		        name,
		        template.toString()
		       );
		final Alias newalias=getAlias(st,name);
		if (newalias==null) {
			throw new SystemConsistencyException("Failed to create alias "+name+" in instance id "+st.getInstance()
			                                                                                         .getId()+", created but not found?");
		}
		return newalias;
	}

	@Nonnull
	@Override
	public String getTableName() {
		return "aliases";
	}

	@Nonnull
	@Override
	public String getIdField() {
		return "aliasid";
	}

	@Nonnull
	@Override
	public String getNameField() {
		return "name";
	}

	@Nullable
	public Instance getInstance() {
		return Instance.get(getInt("instanceid"));
	}

	@Nonnull
	@Override
	public String getLinkTarget() {
		return "/configuration/aliases/view/"+getId();
	}

	@Nonnull
	public JSONObject getTemplate() {
		final String json=dqsnn("select template from aliases where aliasid=?",getId());
		return new JSONObject(json);
	}

	public void setTemplate(@Nonnull final JSONObject template) {
		d("update aliases set template=? where aliasid=?",template.toString(),getId());
	}

	@Nullable
	public String getKVTable() { return null; }

	@Nullable
	public String getKVIdField() { return null; }

	public void flushKVCache(final State st) {}

	public void validate(@Nonnull final State st) {
		if (validated) { return; }
		validate();
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Alias / State Instance mismatch");
		}
	}

	protected int getNameCacheTime() { return 60*60; } // this name doesn't change, cache 1 hour
	// for integrity reasons, renames should be doen through recreates (sadface)

	public void delete() {
		d("delete from aliases where aliasid=?",getId());
	}

}
