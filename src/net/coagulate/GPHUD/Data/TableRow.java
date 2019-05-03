package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.*;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Outputs.Link;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.util.logging.Level.SEVERE;
import static net.coagulate.Core.Tools.UnixTime.getUnixTime;

/**
 * @author Iain Price
 */
public abstract class TableRow extends net.coagulate.Core.Database.TableRow implements Renderable, Comparable {
	public static final int REFRESH_INTERVAL = 60;
	boolean validated = false;
	Map<String, CacheElement> cache = new HashMap<>();

	public TableRow(int id) { super(id); }

	public static String getLink(String name, String target, int id) {
		return new Link(name, "/GPHUD/" + target + "/view/" + id).asHtml(null, true);
	}

	public String getIdColumn() { return getIdField(); }

	public abstract String getIdField();

	@Override
	public final DBConnection getDatabase() { return GPHUD.getDB(); }

	/**
	 * Verify this DB Object has backing in the GPHUD.getDB().
	 * Specifically checks the ID matches one and only one row, as it should.
	 * Only checks once, after which it shorts and returns ASAP. (sets a flag).
	 */
	public void validate() {
		if (validated) { return; }
		int count = dqi(false, "select count(*) from " + getTableName() + " where " + getIdField() + "=?", getId());
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

	public abstract String getNameField();

	public abstract String getLinkTarget();

	public String getName() {
		try { return (String) cacheGet("name"); } catch (CacheMiss ex) {}
		String name = getString(getNameField());
		if (name == null) { return "<null>"; }
		if (name.equals("")) { return "<blank>"; }
		int cachetime = getNameCacheTime();
		if (cachetime == 0) { return name; } // dont cache some things
		return (String) cachePut("name", name, getNameCacheTime());
	}

	protected abstract int getNameCacheTime();

	public String getNameSafe() {
		try {
			return getName();
		} catch (DBException ex) {
			GPHUD.getLogger().log(SEVERE, "SAFE MODE SQLEXCEPTION", ex);
			return "SQLEXCEPTION";
		}
	}

	@Override
	public String toString() { return getNameSafe() + "[#" + getId() + "]"; }

	@Override
	public String asText(State st) {
		return getNameSafe();
	}

	@Override
	public String asHtml(State st, boolean rich) {
		if (!rich) { return getNameSafe(); }
		return getLink(getNameSafe(), getLinkTarget(), getId());
	}

	@Override
	public Set<Renderable> getSubRenderables() { return null; }

	public int resolveToID(State st, String s, boolean instancelocal) {
		boolean debug = false;
		if (s == null) { return 0; }
		if (s.isEmpty()) { return 0; }
		if (debug) { System.out.println("Resolve to id on " + s); }
		try {
			// is it an ID
			int id = Integer.parseInt(s);
			if (id > 0) { return id; }
		} catch (NumberFormatException e) {} // not a number then :P
		try {
			if (debug) { System.out.println("Not an id input"); }
			int id = 0;
			if (instancelocal) {
				id = dqi(true, "select " + getIdField() + " from " + getTableName() + " where " + getNameField() + " like ? and instanceid=?", s, st.getInstance().getId());
			} else {
				id = dqi(true, "select " + getIdField() + " from " + getTableName() + " where " + getNameField() + " like ?", s);
			}
			if (debug) { System.out.println("Not an id input, we resolved to " + id); }
			if (id > 0) { return id; }
		} catch (NoDataException e) {if (debug) { System.out.println("NO DATA");} } catch (TooMuchDataException e) {
			GPHUD.getLogger().warning("Multiple matches searching for " + s + " in " + this.getClass().toString());
		}
		return 0;
	}

	public abstract String getKVTable();

	public abstract String getKVIdField();

	public void kvcheck() {
		if (getKVTable() == null || getKVIdField() == null) {
			throw new SystemException("DBObject " + this.getClass().getName() + " does not support KV mappings");
		}
	}

	public void setKV(State st, String key, String value) {
		kvcheck();
		String oldvalue = dqs(false, "select v from " + getKVTable() + " where " + getKVIdField() + "=? and k like ?", getId(), key);
		if (value == null && oldvalue == null) { return; }
		if (value != null && value.equals(oldvalue)) { return; }
		Modules.validateKV(st, key);
		if (value == null || value.isEmpty()) {
			d("delete from " + getKVTable() + " where " + getKVIdField() + "=? and k like ?", getId(), key);
		} else {
			d("replace into " + getKVTable() + "(" + getKVIdField() + ",k,v) values(?,?,?)", getId(), key, value);
		}
	}

	public Map<String, String> loadKVs() {
		kvcheck();
		Map<String, String> result = new TreeMap<>();
		for (ResultsRow row : dq("select k,v from " + getKVTable() + " where " + getKVIdField() + "=?", getId())) {
			result.put(row.getString("k").toLowerCase(), row.getString("v"));
		}
		return result;
	}

	@Override
	/** Provide a sorting order based on names.
	 * Implements the comparison operator for sorting (TreeSet etc)
	 * We rely on the names as the sorting order, and pass the buck to String.compareTo()
	 */
	public int compareTo(Object t) {
		if (!TableRow.class.isAssignableFrom(t.getClass())) {
			throw new SystemException(t.getClass().getName() + " is not assignable from DBObject");
		}
		String ours = getNameSafe();
		TableRow them = (TableRow) t;
		String theirs = them.getNameSafe();
		return ours.compareTo(theirs);
	}

	Object cacheGet(String key) throws CacheMiss {
		if (!cache.containsKey(key)) { throw new CacheMiss(); }
		CacheElement ele = cache.get(key);
		if (ele.expires < getUnixTime()) {
			cache.remove(key);
			throw new CacheMiss();
		}
		return ele.element;
	}

	Object cachePut(String key, Object object, int lifetimeseconds) {
		CacheElement ele = new CacheElement(object, getUnixTime() + lifetimeseconds);
		cache.put(key, ele);
		return object;
	}

	private static class CacheElement {
		public Object element;
		public int expires;

		public CacheElement(Object element, int expires) {
			this.element = element;
			this.expires = expires;
		}
	}

	protected static class CacheMiss extends Exception {
	}
}
