package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.*;
import net.coagulate.Core.Exceptions.SystemException;
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
	public static final int REFRESH_INTERVAL = 60;
	boolean validated;
	final Map<String, CacheElement> cache = new HashMap<>();

	public TableRow(final int id) { super(id); }

	@Nonnull
	public static String getLink(final String name, final String target, final int id) {
		return new Link(name, "/GPHUD/" + target + "/view/" + id).asHtml(null, true);
	}

	@Nonnull
	public String getIdColumn() { return getIdField(); }

	@Nonnull
	public abstract String getIdField();

	@Nonnull
	@Override
	public final DBConnection getDatabase() { return GPHUD.getDB(); }

	/**
	 * Verify this DB Object has backing in the GPHUD.getDB().
	 * Specifically checks the ID matches one and only one row, as it should.
	 * Only checks once, after which it shorts and returns ASAP. (sets a flag).
	 */
	public void validate() {
		if (validated) { return; }
		final int count = dqi( "select count(*) from " + getTableName() + " where " + getIdField() + "=?", getId());
		if (count > 1) {
			throw new TooMuchDataException("Too many rows - got " + count + " instead of 1 while validating " + getTableName() + " - " + getId());
		}
		if (count < 1) {
			throw new NoDataException("No rows - got " + count + " instead of 1 while validating " + getTableName() + " - " + getId());
		}
		validated = true;
	}

	/**
	 * Validate with respect to a given state.
	 * This is intended to ensure that 'things' make sense, such as the Character is part of the State's instance etc.
	 *
	 * @param st State
	 * @throws SystemException If there is a mismatch between state and this object
	 */
	public abstract void validate(State st) throws SystemException;

	@Nullable
	public abstract String getNameField();

	@Nullable
	public abstract String getLinkTarget();

	@Nonnull
	public String getName() {
		try { return (String) cacheGet("name"); } catch (final CacheMiss ex) {}
		final String name = getString(getNameField());
		if (name == null) { return "<null>"; }
		if ("".equals(name)) { return "<blank>"; }
		final int cachetime = getNameCacheTime();
		if (cachetime == 0) { return name; } // dont cache some things
		return (String) cachePut("name", name, getNameCacheTime());
	}

	protected abstract int getNameCacheTime();

	@Nullable
	public String getNameSafe() {
		try {
			return getName();
		} catch (final DBException ex) {
			GPHUD.getLogger().log(SEVERE, "SAFE MODE SQLEXCEPTION", ex);
			return "SQLEXCEPTION";
		}
	}

	@Nonnull
	@Override
	public String toString() { return getNameSafe() + "[#" + getId() + "]"; }

	@Nonnull
	@Override
	public String asText(final State st) {
		return getNameSafe();
	}

	@Nonnull
	@Override
	public String asHtml(final State st, final boolean rich) {
		if (!rich) { return getNameSafe(); }
		return getLink(getNameSafe(), getLinkTarget(), getId());
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() { return null; }

	public int resolveToID(@Nonnull final State st, @Nullable final String s, final boolean instancelocal) {
		final boolean debug = false;
		if (s == null) { return 0; }
		if (s.isEmpty()) { return 0; }
		try {
			// is it an ID
			final int id = Integer.parseInt(s);
			if (id > 0) { return id; }
		} catch (final NumberFormatException e) {} // not a number then :P
		try {
			int id = 0;
			if (instancelocal) {
				id = dqi( "select " + getIdField() + " from " + getTableName() + " where " + getNameField() + " like ? and instanceid=?", s, st.getInstance().getId());
			} else {
				id = dqi( "select " + getIdField() + " from " + getTableName() + " where " + getNameField() + " like ?", s);
			}
			if (id > 0) { return id; }
		} catch (final NoDataException e) { } catch (final TooMuchDataException e) {
			GPHUD.getLogger().warning("Multiple matches searching for " + s + " in " + getClass());
		}
		return 0;
	}

	@Nullable
	public abstract String getKVTable();

	@Nullable
	public abstract String getKVIdField();

	public void kvcheck() {
		if (getKVTable() == null || getKVIdField() == null) {
			throw new SystemException("DBObject " + getClass().getName() + " does not support KV mappings");
		}
	}

	public void setKV(final State st, @Nonnull final String key, @Nullable final String value) {
		kvcheck();
		String oldvalue = null;
		try { oldvalue=dqs( "select v from " + getKVTable() + " where " + getKVIdField() + "=? and k like ?", getId(), key); } catch (final NoDataException e) {}
		if (value == null && oldvalue == null) { return; }
		if (value != null && value.equals(oldvalue)) { return; }
		Modules.validateKV(st, key);
		if (value == null || value.isEmpty()) {
			d("delete from " + getKVTable() + " where " + getKVIdField() + "=? and k like ?", getId(), key);
		} else {
			d("replace into " + getKVTable() + "(" + getKVIdField() + ",k,v) values(?,?,?)", getId(), key, value);
		}
	}

	@Nonnull
	public Map<String, String> loadKVs() {
		kvcheck();
		final Map<String, String> result = new TreeMap<>();
		for (final ResultsRow row : dq("select k,v from " + getKVTable() + " where " + getKVIdField() + "=?", getId())) {
			result.put(row.getStringNullable("k").toLowerCase(), row.getStringNullable("v"));
		}
		return result;
	}

	@Override
	/** Provide a sorting order based on names.
	 * Implements the comparison operator for sorting (TreeSet etc)
	 * We rely on the names as the sorting order, and pass the buck to String.compareTo()
	 */
	public int compareTo(@Nonnull final TableRow t) {
		if (!TableRow.class.isAssignableFrom(t.getClass())) {
			throw new SystemException(t.getClass().getName() + " is not assignable from DBObject");
		}
		final String ours = getNameSafe();
		final String theirs = t.getNameSafe();
		return ours.compareTo(theirs);
	}

	Object cacheGet(final String key) throws CacheMiss {
		if (!cache.containsKey(key)) { throw new CacheMiss(); }
		final CacheElement ele = cache.get(key);
		if (ele==null) { throw new CacheMiss(); }
		if (ele.expires < getUnixTime()) {
			cache.remove(key);
			throw new CacheMiss();
		}
		return ele.element;
	}

	Object cachePut(final String key, final Object object, final int lifetimeseconds) {
		final CacheElement ele = new CacheElement(object, getUnixTime() + lifetimeseconds);
		cache.put(key, ele);
		return object;
	}

	private static class CacheElement {
		public final Object element;
		public final int expires;

		public CacheElement(final Object element, final int expires) {
			this.element = element;
			this.expires = expires;
		}
	}

	protected static class CacheMiss extends Exception {
		private static final long serialVersionUID=1L;
	}
}
