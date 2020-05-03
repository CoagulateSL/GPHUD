package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Database.TooMuchDataException;
import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Outputs.Link;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.util.logging.Level.SEVERE;
import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * @author Iain Price
 */
public abstract class TableRow extends net.coagulate.Core.Database.TableRow implements Renderable, Comparable<TableRow> {
	public static final int REFRESH_INTERVAL=60;
	final Map<String,CacheElement> cache=new HashMap<>();
	boolean validated;

	public TableRow(final int id) { super(id); }

	protected TableRow() {
		super();
	}

	// ---------- STATICS ----------
	public static final DBConnection db() { return GPHUD.getDB(); }

	/**
	 * Returns a formatted view link.
	 *
	 * @param name   Name, for display purposes
	 * @param target target type
	 * @param id     id of target
	 *
	 * @return Link to /GPHUD/target/view/id
	 */
	@Nonnull
	public static String getLink(@Nonnull final String name,
	                             @Nonnull final String target,
	                             final int id) {
		return new Link(name,"/GPHUD/"+target+"/view/"+id).asHtml(null,true);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public abstract String getIdColumn();

	@Nonnull
	@Override
	public final DBConnection getDatabase() { return db(); }

	/**
	 * Verify this DB Object has backing in the getDB().
	 * Specifically checks the ID matches one and only one row, as it should.
	 * Only checks once, after which it shorts and returns ASAP. (sets a flag).
	 */
	public void validate() {
		if (validated) { return; }
		final int count=dqinn("select count(*) from "+getTableName()+" where "+getIdColumn()+"=?",getId());
		if (count>1) {
			throw new TooMuchDataException("Too many rows - got "+count+" instead of 1 while validating "+getTableName()+" - "+getId());
		}
		if (count<1) {
			throw new NoDataException("No rows - got "+count+" instead of 1 while validating "+getTableName()+" - "+getId());
		}
		validated=true;
	}

	/**
	 * Validate with respect to a given state.
	 * This is intended to ensure that 'things' make sense, such as the Character is part of the State's instance etc.
	 *
	 * @param st State
	 *
	 * @throws SystemConsistencyException If there is a mismatch between state and this object
	 */
	public abstract void validate(@Nonnull State st);

	@Nullable
	public abstract String getNameField();

	@Nullable
	public abstract String getLinkTarget();

	/**
	 * Gets the name of this object, optionally using the cache.
	 *
	 * @return The name, or null or blank if null or blank
	 */
	@Nonnull
	public String getName() {
		if (getNameField()==null) { throw new SystemConsistencyException("Getting name of something with a null getNameField()"); }
		try { return (String) cacheGet("name"); } catch (@Nonnull final CacheMiss ex) {}
		final String name=getStringNullable(getNameField());
		if (name==null) { return "<null>"; }
		if ("".equals(name)) { return "<blank>"; }
		final int cachetime=getNameCacheTime();
		if (cachetime==0) { return name; } // dont cache some things
		return (String) cachePut("name",name,getNameCacheTime());
	}

	/**
	 * Highly protected version of getName() that never fails.
	 *
	 * @return The name of the object, as per getName(), or SQLEXCEPTION (logged separately) if an error occurs
	 */
	@Nonnull
	public String getNameSafe() {
		try {
			return getName();
		}
		catch (@Nonnull final Throwable ex) {
			GPHUD.getLogger().log(SEVERE,"SAFE MODE SQLEXCEPTION",ex);
			return "EXCEPTION";
		}
	}

	@Nonnull
	@Override
	public String toString() { return getNameSafe()+"[#"+getId()+"]"; }

	/**
	 * Convert this object to text.
	 *
	 * @param st Unused state
	 *
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
	 *
	 * @return The name, via getNameSafe, with link if rich mode
	 */
	@Nonnull
	@Override
	public String asHtml(@Nonnull final State st,
	                     final boolean rich) {
		if (!rich || getLinkTarget()==null) { return getNameSafe(); }
		return getLink(getNameSafe(),getLinkTarget(),getId());
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() { return null; }

	@Nullable
	public abstract String getKVTable();

	@Nullable
	public abstract String getKVIdField();

	/**
	 * Set a KV value for this object
	 *
	 * @param st    State
	 * @param key   The K in KV
	 * @param value The V in KV, may be null or "" to clear the value
	 */
	public void setKV(final State st,
	                  @Nonnull final String key,
	                  @Nullable final String value) {
		kvcheck();
		String oldvalue=null;
		try {
			oldvalue=dqs("select v from "+getKVTable()+" where "+getKVIdField()+"=? and k like ?",getId(),key);
		}
		catch (@Nonnull final NoDataException e) {}
		if (value==null && oldvalue==null) { return; }
		if (value!=null && value.equals(oldvalue)) { return; }
		Modules.validateKV(st,key);
		if (value==null || value.isEmpty()) {
			d("delete from "+getKVTable()+" where "+getKVIdField()+"=? and k like ?",getId(),key);
		}
		else {
			d("replace into "+getKVTable()+"("+getKVIdField()+",k,v) values(?,?,?)",getId(),key,value);
		}
	}

	/**
	 * Load all KV mappings for this object.
	 *
	 * @return A Map of String K to String V pairs.
	 */
	@Nonnull
	public Map<String,String> loadKVs() {
		kvcheck();
		final Map<String,String> result=new TreeMap<>();
		for (final ResultsRow row: dq("select k,v from "+getKVTable()+" where "+getKVIdField()+"=?",getId())) {
			result.put(row.getString("k").toLowerCase(),row.getString("v"));
		}
		return result;
	}

	/**
	 * Provide a sorting order based on names.
	 * Implements the comparison operator for sorting (TreeSet etc)
	 * We rely on the names as the sorting order, and pass the buck to String.compareTo()
	 */
	@Override
	public int compareTo(@Nonnull final TableRow t) {
		/*if (!TableRow.class.isAssignableFrom(t.getClass())) {
			throw new SystemImplementationException(t.getClass().getName() + " is not assignable from DBObject");
		}*/
		final String ours=getNameSafe();
		final String theirs=t.getNameSafe();
		return ours.compareTo(theirs);
	}

	// ----- Internal Instance -----

	/**
	 * Package access method to resolve a name to an ID number
	 *
	 * @param st            State
	 * @param name          Name to look up
	 * @param instancelocal in the state's instance or globally if false
	 *
	 * @return the ID number of the matching record, or zero if there is no match
	 */
	int resolveToID(@Nonnull final State st,
	                @Nullable final String name,
	                final boolean instancelocal) {
		final boolean debug=false;
		if (name==null) { return 0; }
		if (name.isEmpty()) { return 0; }
		try {
			// is it an ID
			final int id=Integer.parseInt(name);
			if (id>0) { return id; }
		}
		catch (@Nonnull final NumberFormatException e) {} // not a number then :P
		try {
			final int id;
			if (instancelocal) {
				id=dqinn("select "+getIdColumn()+" from "+getTableName()+" where "+getNameField()+" like ? and instanceid=?",name,st.getInstance().getId());
			}
			else {
				id=dqinn("select "+getIdColumn()+" from "+getTableName()+" where "+getNameField()+" like ?",name);
			}
			if (id>0) { return id; }
		}
		catch (@Nonnull final NoDataException e) { }
		catch (@Nonnull final TooMuchDataException e) {
			GPHUD.getLogger().warning("Multiple matches searching for "+name+" in "+getClass());
		}
		return 0;
	}

	/**
	 * Exception if any of the KV configuration is nulled
	 */
	void kvcheck() {
		if (getKVTable()==null || getKVIdField()==null) {
			throw new SystemImplementationException("DBObject "+getClass().getName()+" does not support KV mappings");
		}
	}

	/**
	 * Get an object from the cache
	 *
	 * @param key Cache key
	 *
	 * @return Object from the cache
	 *
	 * @throws CacheMiss If there is no cached object by that key
	 */
	@Nonnull
	Object cacheGet(@Nonnull final String key) throws CacheMiss {
		if (!cache.containsKey(key)) { throw new CacheMiss(); }
		final CacheElement ele=cache.get(key);
		if (ele==null) { throw new CacheMiss(); }
		if (ele.expires<getUnixTime()) {
			cache.remove(key);
			throw new CacheMiss();
		}
		return ele.element;
	}

	/**
	 * Store an element in the cache
	 *
	 * @param key             Cache key
	 * @param object          Cache element
	 * @param lifetimeseconds How long to cache for in seconds
	 *
	 * @return The object being cached (object)
	 */
	@Nonnull
	Object cachePut(@Nonnull final String key,
	                @Nonnull final Object object,
	                final int lifetimeseconds) {
		final CacheElement ele=new CacheElement(object,getUnixTime()+lifetimeseconds);
		cache.put(key,ele);
		return object;
	}

	/**
	 * The cache time in seconds for the name of this object
	 *
	 * @return Cache time in seconds
	 */
	protected abstract int getNameCacheTime();

	private static class CacheElement {
		@Nonnull
		public final Object element;
		public final int expires;

		public CacheElement(@Nonnull final Object element,
		                    final int expires) {
			this.element=element;
			this.expires=expires;
		}
	}

	protected static class CacheMiss extends Exception {
		private static final long serialVersionUID=1L;
	}
}
