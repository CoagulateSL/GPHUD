package net.coagulate.GPHUD.Data;

import java.util.Map;
import java.util.TreeMap;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.GPHUD.GPHUD;

/** Implements a naming cache, please read the warnings.
 * Warning:  GPHUD is not intended to use caching - the database can be updated by external systems (e.g. the other node).
 * As such caches may become out of date.
 * However, the Audit page contains vast numbers of numeric ID to name lookups, and rather than "miss-cache" or not cache these lookups (about a dozen per Audit record)
 * we have a cache here, that pre loads with the name to ID mappings, when each type is first accessed.
 * DO NOT PERSIST THIS OBJECT, it's intended to be used for a limited scope and then discarded.  Do not store it in a static persistent reference.
 * @author Iain Price <gphud@predestined.net>
 */
public class NameCache {

    Map<Integer,String> usernames=null;
    Map<Integer,String> avatarnames=null;
    Map<Integer,String> characternames=null;
    Map<Integer,String> instancenames=null;
    Map<Integer,String> regionnames=null;
    

    public String lookup(User u) {
        if (usernames==null) { usernames=loadMap("users","userid","username"); }
        return usernames.get(u.getId());
    }
    public String lookup(Avatar u) {
        if (avatarnames==null) { avatarnames=loadMap("avatars","avatarid","avatarname"); }
        return avatarnames.get(u.getId());
    }
    public String lookup(Char u) {
        if (characternames==null) { characternames=loadMap("characters","characterid","name"); }
        return characternames.get(u.getId());
    }
    public String lookup(Instance u) {
        if (instancenames==null) { instancenames=loadMap("instances","instanceid","name"); }
        return instancenames.get(u.getId());
    }
    public String lookup(Region u) {
        if (regionnames==null) { regionnames=loadMap("regions","regionid","name"); }
        return regionnames.get(u.getId());
    }
    
    
    private static Map<Integer,String> loadMap(String tablename,String idcolumn,String namecolumn){
        Map<Integer,String> results=new TreeMap<>();
        Results rows=GPHUD.getDB().dq("select "+idcolumn+","+namecolumn+" from "+tablename);
        for (ResultsRow r:rows) {
            results.put(r.getInt(idcolumn),TableRow.getLink(r.getString(namecolumn), tablename, r.getInt(idcolumn)));
        }
        return results;
    }
}
