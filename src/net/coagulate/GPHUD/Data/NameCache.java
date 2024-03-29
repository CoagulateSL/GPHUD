package net.coagulate.GPHUD.Data;

import net.coagulate.Core.Database.DBConnection;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.SL.Data.User;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Implements a naming cache, please read the warnings.
 * <p>
 * PRIMARY USE: Audit display
 * <p>
 * Warning:  GPHUD is not intended to use caching - the database can be updated by external systems (e.g. the other node).
 * As such caches may become out of date.
 * However, the Audit page contains vast numbers of numeric ID to name lookups, and rather than "miss-cache" or not cache these lookups (about a dozen per Audit record)
 * we have a cache here, that pre loads with the name to ID mappings, when each type is first accessed.
 * DO NOT PERSIST THIS OBJECT, it's intended to be used for a limited scope and then discarded.  Do not store it in a static persistent reference.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class NameCache {
	@Nullable Map<Integer,String> avatarnames;
	@Nullable Map<Integer,String> characternames;
	@Nullable Map<Integer,String> instancenames;
	@Nullable Map<Integer,String> regionnames;
	
	// ---------- INSTANCE ----------
	@Nullable
	public String lookup(@Nonnull final User u) {
		if (avatarnames==null) {
			avatarnames=User.loadMap();
		}
		return avatarnames.get(u.getId());
	}
	
	@Nullable
	public String lookup(@Nonnull final Char u) {
		if (characternames==null) {
			characternames=loadMap("characters","characterid","name");
		}
		return characternames.get(u.getId());
	}
	
	@Nonnull
	private static Map<Integer,String> loadMap(@Nonnull final String tablename,
	                                           @Nonnull final String idcolumn,
	                                           @Nonnull final String namecolumn) {
		final Map<Integer,String> results=new TreeMap<>();
		final Results rows=db().dq("select "+idcolumn+","+namecolumn+" from "+tablename);
		for (final ResultsRow r: rows) {
			results.put(r.getInt(idcolumn),
			            TableRow.getLink(r.getString(namecolumn).replaceAll(" ","&nbsp;"),
			                             tablename,
			                             r.getInt(idcolumn)));
		}
		return results;
	}
	
	// ----- Internal Statics -----
	private static DBConnection db() {
		return GPHUD.getDB();
	}
	
	@Nullable
	public String lookup(@Nonnull final Instance u) {
		if (instancenames==null) {
			instancenames=loadMap("instances","instanceid","name");
		}
		return instancenames.get(u.getId());
	}
	
	@Nullable
	public String lookup(@Nonnull final Region u) {
		if (regionnames==null) {
			regionnames=loadMap("regions","regionid","name");
		}
		return regionnames.get(u.getId());
	}
}
