package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputDuplicateValueException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.CacheConfig;
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
	
	private static final Cache<Instance,Map<String,Alias>> aliasMapCache=
			Cache.getCache("GPHUD/aliasMap",CacheConfig.OPERATIONAL_CONFIG);
	
	// ---------- STATICS ----------
	private static final Cache<Instance,Map<String,JSONObject>> aliasTemplateCache=
			Cache.getCache("GPHUD/aliasTemplate",CacheConfig.OPERATIONAL_CONFIG);
	private static final Cache<Alias,Instance> instanceCache=
			Cache.getCache("GPHUD/AliasInstance",CacheConfig.PERMANENT_CONFIG);
	
	/**
	 * Returns a map of aliases for this state.
	 *
	 * @param st State
	 * @return Map of Alias Name to Alias objects
	 */
	@Nonnull
	public static Map<String,Alias> getAliasMap(@Nonnull final State st) {
		return aliasMapCache.get(st.getInstance(),()->{
			final Map<String,Alias> aliases=new TreeMap<>();
			for (final ResultsRow r: db().dq("select name,aliasid from aliases where instanceid=?",
			                                 st.getInstance().getId())) {
				aliases.put(r.getStringNullable("name"),get(r.getInt("aliasid")));
			}
			return aliases;
		});
	}
	
	/**
	 * Factory style constructor
	 *
	 * @param id the ID number we want to get
	 * @return An Alias representation
	 */
	@Nonnull
	public static Alias get(final int id) {
		return (Alias)factoryPut("Alias",id,Alias::new);
	}
	
	protected Alias(final int id) {
		super(id);
	}
	
	/**
	 * Get aliased command templates for this state
	 *
	 * @param st State
	 * @return Map of Name to Template (JSON) mappings
	 */
	@Nonnull
	public static Map<String,JSONObject> getTemplates(@Nonnull final State st) {
		return aliasTemplateCache.get(st.getInstance(),()->{
			final Map<String,JSONObject> aliases=new TreeMap<>();
			for (final ResultsRow r: db().dq("select name,template from aliases where instanceid=?",
			                                 st.getInstance().getId())) {
				aliases.put(r.getString("name"),new JSONObject(r.getString("template")));
			}
			return aliases;
		});
	}
	
	private static void clearCaches(final Instance instance) {
		aliasTemplateCache.purge(instance);
		aliasMapCache.purge(instance);
	}
	
	/**
	 * Create a new alias.
	 * Protects against weird name inputs and duplicate aliases.
	 *
	 * @param st       State
	 * @param name     Short name for new alias
	 * @param template JSON Template of alias (See Alias Module)
	 * @return A reference to the newly created alias
	 *
	 * @throws UserInputValidationParseException if the name contains illegal characters
	 * @throws UserInputDuplicateValueException  if the name is already taken
	 */
	@Nonnull
	public static Alias create(@Nonnull final State st,@Nonnull final String name,@Nonnull final JSONObject template) {
		if (getAlias(st,name)!=null) {
			throw new UserInputDuplicateValueException("Alias "+name+" already exists");
		}
		if (name.matches(".*[^A-Za-z\\d-=_,].*")) {
			throw new UserInputValidationParseException(
					"Aliases must not contain spaces, and mostly only allow A-Z a-z 0-9 - + _ ,");
		}
		db().d("insert into aliases(instanceid,name,template) values(?,?,?)",
		       st.getInstance().getId(),
		       name,
		       template.toString());
		final Alias newAlias=getAlias(st,name);
		if (newAlias==null) {
			throw new SystemConsistencyException(
					"Failed to create alias "+name+" in instance id "+st.getInstance().getId()+
					", created but not found?");
		}
		clearCaches(st.getInstance());
		return newAlias;
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String getTableName() {
		return "aliases";
	}
	
	@Nonnull
	@Override
	public String getIdColumn() {
		return "aliasid";
	}
	
	/**
	 * Get a particular alias from an instance.
	 *
	 * @param st   State
	 * @param name Short Name of alias
	 * @return The Alias object, or null if not found.
	 */
	@Nullable
	public static Alias getAlias(@Nonnull final State st,final String name) {
		try {
			final int id=db().dqiNotNull("select aliasid from aliases where instanceid=? and name like ?",
			                             st.getInstance().getId(),
			                             name);
			return get(id);
		} catch (@Nonnull final NoDataException e) {
			return null;
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
		return "/configuration/aliases/view/"+getId();
	}
	
	public void validate(@Nonnull final State st) {
		try {
			validate();
		} catch (final NoDataException e) {
			throw new UserInputLookupFailureException("Can not find alias #"+getId(),e,true);
		}
		if (st.getInstance()!=getInstance()) {
			throw new SystemConsistencyException("Alias / State Instance mismatch");
		}
	}
	
	@Nullable
	public String getKVTable() {
		return null;
	}
	
	@Nullable
	public String getKVIdField() {
		return null;
	}
	
	@Nullable
	public Instance getInstance() {
		return instanceCache.get(this,()->Instance.get(getInt("instanceid")));
	}
	
	protected int getNameCacheTime() {
		return 60*60;
	} // this name doesn't change, cache 1 hour
	
	/**
	 * Gets the JSON payload for this alias.  See alias module.
	 *
	 * @return The JSON template for this alias.
	 */
	@Nonnull
	public JSONObject getTemplate() {
		final String json=dqsnn("select template from aliases where aliasid=?",getId());
		return new JSONObject(json);
	}
	
	/**
	 * Set the JSON template for this alias.
	 *
	 * @param template The new JSON template for this alias.
	 */
	public void setTemplate(@Nonnull final JSONObject template) {
		d("update aliases set template=? where aliasid=?",template.toString(),getId());
		clearCaches(getInstance());
	}
	
	public void delete() {
		d("delete from aliases where aliasid=?",getId());
		clearCaches(getInstance());
	}
	
}
