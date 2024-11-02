package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Database.TooMuchDataException;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Tools.Cache;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Outputs.Link;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.util.logging.Level.SEVERE;

/**
 * @author Iain Price
 */
public abstract class TableRow extends net.coagulate.Core.Database.TableRow implements Renderable, Comparable<TableRow> {
	public static final int REFRESH_INTERVAL=60;
	
	protected boolean validated;
	
	protected TableRow(final int id) {
		super(id);
	}
	
	protected static final Cache<TableRow,Map<String,String>> kvCache=
			Cache.getCache("GPHUD/kvCache",CacheConfig.DURABLE_CONFIG);
	
	protected TableRow() {
	}
	
	@Nonnull
	@Override
	public final DBConnection getDatabase() {
		return db();
	}
	
	// ---------- STATICS ----------
	public static DBConnection db() {
		return GPHUD.getDB();
	}
	
	/**
	 * Verify this DB Object has backing in the getDB().
	 * Specifically checks the ID matches one and only one row, as it should.
	 * Only checks once, after which it shorts and returns ASAP. (sets a flag).
	 */
	protected void validate() {
		if (validated) {
			return;
		}
		final int count=dqinn("select count(*) from "+getTableName()+" where "+getIdColumn()+"=?",getId());
		if (count>1) {
			throw new TooMuchDataException(
					"Too many rows - got "+count+" instead of 1 while validating "+getTableName()+" - "+getId());
		}
		if (count<1) {
			throw new NoDataException(
					"No rows - got "+count+" instead of 1 while validating "+getTableName()+" - "+getId());
		}
		validated=true;
	}
	
	/**
	 * Validate with respect to a given state.
	 * This is intended to ensure that 'things' make sense, such as the Character is part of the State's instance etc.
	 *
	 * @param st State
	 * @throws SystemConsistencyException If there is a mismatch between state and this object
	 */
	public abstract void validate(@Nonnull State st);
	
	@Nullable
	public abstract String getNameField();
	
	@Nullable
	public abstract String getLinkTarget();
	
	protected void clearNameCache() {
		nameCache.purge(this);
	}
	
	@Nonnull
	@Override
	public String toString() {
		return getNameSafe()+"[#"+getId()+"]";
	}
	
	private static final Cache<TableRow,String> nameCache=Cache.getCache("GPHUD/nameCache",CacheConfig.PERMANENT_CONFIG);
	
	/**
	 * Highly protected version of getName() that never fails.
	 *
	 * @return The name of the object, as per getName(), or SQLEXCEPTION (logged separately) if an error occurs
	 */
	@Nonnull
	public String getNameSafe() {
		try {
			return getName();
		} catch (@Nonnull final NoDataException nde) {
			return "NOTFOUND#"+getId();
		} catch (@Nonnull final Throwable ex) {
			GPHUD.getLogger().log(SEVERE,"SAFE MODE SQLEXCEPTION",ex);
			return "EXCEPTION";
		}
	}
	
	/**
	 * Gets the name of this object, optionally using the cache.
	 *
	 * @return The name, or null or blank if null or blank
	 */
	@Nonnull
	public String getName() {
		return nameCache.get(this,()->{
			if (getNameField()==null) {
				throw new SystemConsistencyException("Getting name of something with a null getNameField()");
			}
			return getStringNullable(getNameField());
		});
	}
	protected void setNameCache(@Nonnull final String value) { nameCache.set(this,value); }
	public void setName(@Nonnull final String value) {
		if (getNameField()==null) {
			throw new SystemConsistencyException("Setting name of something with a null getNameField()");
		}
		set(getNameField(),value);
		nameCache.set(this,value);
	}
	
	/**
	 * Convert this object to text.
	 *
	 * @param st Unused state
	 * @return the name via getNameSafe()
	 */
	@Nonnull
	@Override
	public String asText(@Nonnull final State st) {
		return getNameSafe();
	}
	
	/**
	 * This object formatted as HTML.
	 *
	 * @param st   state
	 * @param rich Rich mode
	 * @return The name, via getNameSafe, with link if rich mode
	 */
	@Nonnull
	@Override
	public String asHtml(@Nonnull final State st,final boolean rich) {
		if (!rich||getLinkTarget()==null) {
			return getNameSafe();
		}
		return getLink(getNameSafe(),getLinkTarget(),getId());
	}
	
	/**
	 * Returns a formatted view link.
	 *
	 * @param name   Name, for display purposes
	 * @param target target type
	 * @param id     id of target
	 * @return Link to /GPHUD/target/view/id
	 */
	@Nonnull
	public static String getLink(@Nonnull final String name,@Nonnull final String target,final int id) {
		return new Link(name,"/GPHUD/"+target+"/view/"+id).asHtml(null,true);
	}
	
	@Nullable
	public abstract String getKVTable();
	
	@Nullable
	public abstract String getKVIdField();
	
	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}
	
	/**
	 * Set a KV value for this object
	 *
	 * @param st    State
	 * @param key   The K in KV
	 * @param value The V in KV, may be null or "" to clear the value
	 */
	public void setKV(final State st,@Nonnull final String key,@Nullable final String value) {
		kvCheck();
		final String oldValue=loadKVs().get(key.toLowerCase());
		if (value==null&&oldValue==null) {
			return;
		}
		if (value!=null&&value.equals(oldValue)) {
			return;
		}
		Modules.validateKV(st,key);
		if (value==null||value.isEmpty()) {
			d("delete from "+getKVTable()+" where "+getKVIdField()+"=? and k like ?",getId(),key);
		} else {
			d("replace into "+getKVTable()+"("+getKVIdField()+",k,v) values(?,?,?)",getId(),key,value);
		}
		loadKVs().put(key.toLowerCase(),value);
	}
	
	/**
	 * Exception if any of the KV configuration is nulled
	 */
	void kvCheck() {
		if (getKVTable()==null||getKVIdField()==null) {
			throw new SystemImplementationException("DBObject "+getClass().getName()+" does not support KV mappings");
		}
	}
	
	/**
	 * Provide a sorting order based on names.
	 * Implements the comparison operator for sorting (TreeSet etc)
	 * We rely on the names as the sorting order, and pass the buck to String.compareTo()
	 */
	@Override
	public int compareTo(@Nonnull final TableRow t) {
		final String ours=getNameSafe();
		final String theirs=t.getNameSafe();
		return ours.compareTo(theirs);
	}
	
	// ----- Internal Instance -----
	
	/**
	 * Load all KV mappings for this object.
	 *
	 * @return A Map of String K to String V pairs.
	 */
	@Nonnull
	public Map<String,String> loadKVs() {
		return kvCache.get(this,()->{
			kvCheck();
			final Map<String,String> result=new TreeMap<>();
			for (final ResultsRow row: dq("select k,v from "+getKVTable()+" where "+getKVIdField()+"=?",getId())) {
				result.put(row.getString("k").toLowerCase(),row.getString("v"));
			}
			return result;
		});
	}
	
	/**
	 * Package access method to resolve a name to an ID number
	 *
	 * @param st            State
	 * @param name          Name to look up
	 * @param instanceLocal in the state's instance or globally if false
	 * @return the ID number of the matching record, or zero if there is no match
	 */
	int resolveToID(@Nonnull final State st,@Nullable final String name,final boolean instanceLocal) {
		if (name==null) {
			return 0;
		}
		if (name.isEmpty()) {
			return 0;
		}
		try {
			// is it an ID
			final int id=Integer.parseInt(name);
			if (id>0) {
				return id;
			}
		} catch (@Nonnull final NumberFormatException ignored) {
		} // not a number then :P
		try {
			final int id;
			if (instanceLocal) {
				id=dqinn("select "+getIdColumn()+" from "+getTableName()+" where "+getNameField()+
				         " like ? and instanceid=?",name,st.getInstance().getId());
			} else {
				id=dqinn("select "+getIdColumn()+" from "+getTableName()+" where "+getNameField()+" like ?",name);
			}
			if (id>0) {
				return id;
			}
		} catch (@Nonnull final NoDataException ignored) {
		} catch (@Nonnull final TooMuchDataException e) {
			GPHUD.getLogger().warning("Multiple matches searching for "+name+" in "+getClass());
		}
		return 0;
	}
}
