package net.coagulate.GPHUD.Data;

import java.util.Set;
import java.util.TreeSet;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import org.json.JSONObject;

/** Reference to a character group.
 * A group of characters within an instance, can represent factions, classes or other groupings.
 * @author Iain Price <gphud@predestined.net>
 */
public class CharacterGroup extends TableRow {

    static void wipeKV(Instance instance, String key) {
        String kvtable="charactergroupkvstore";
        String maintable="charactergroups";
        String idcolumn="charactergroupid";
        GPHUD.getDB().d("delete from "+kvtable+" using "+kvtable+","+maintable+" where "+kvtable+".k like ? and "+kvtable+"."+idcolumn+"="+maintable+"."+idcolumn+" and "+maintable+".instanceid=?",key,instance.getId());
    }

    public static JSONObject createChoice(State st, Attribute a) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    // open groups only
    public static void createChoice(State st, JSONObject json,String prefix,Attribute a) {
        final boolean debug=false;
        json.put(prefix+"type","SELECT");
        json.put(prefix+"name","value");
        int count=0;
        for (CharacterGroup cg:st.getInstance().getGroupsForKeyword(a.getSubType())) {
            System.out.println("Scanning CG "+cg.getNameSafe());
            if (cg.isOpen()) {
                json.put(prefix+"button"+count, cg.getName());
                count++;
            }
        }
    }

    /** Get group by name
     * 
     * @param st State
     * @param name Name of group
     * @return CharacterGroup
     */
    public static CharacterGroup resolve(State st,String name) {
        int id=new CharacterGroup(-1).resolveToID(st,name,true);
        if (id==0) { return null; }
        return get(id);
    }
    
    @Override
    public String getLinkTarget() { return "charactergroup"; }    
    /** Factory style constructor
     * 
     * @param id the ID number we want to get
     * @return A Region representation
     */
    public static CharacterGroup get(int id) {
        return (CharacterGroup)factoryPut("CharacterGroup",id,new CharacterGroup(id));
    }
    
    protected CharacterGroup(int id) { super(id); }

    /** Find a region by name.
     * 
     * @param name Name of region to locate
     * @return Region object for that region, or null if none is found.
     */
    public static CharacterGroup find(String name,Instance i) {
        Integer id=GPHUD.getDB().dqi(false,"select charactergroupid from charactergroups where name like ? and instanceid=?",name,i.getId());
        if (id==null) { return null; }
        return get(id);
    }
    
    /** Gets the instance associated with this region
     * 
     * @return The Instance object
     */
    public Instance getInstance() {
        return Instance.get(getInt("instanceid"));
    }

    @Override
    public String getTableName() {
        return "charactergroups";
    }

    @Override
    public String getIdField() {
        return "charactergroupid";
    }

    @Override
    public String getNameField() {
        return "name";
    }

    /** Get the members of the group
     * 
     * @return  Set of Characters
     */
    public Set<Char> getMembers() {
        Set<Char> members=new TreeSet<>();
        Results results=dq("select characterid from charactergroupmembers where charactergroupid=?",getId());
        for (ResultsRow r:results) {
            Char record=Char.get(r.getInt());
            members.add(record);
        }
        return members;
    }

    /** Delete this character group
     * 
     */
    public void delete() {
        d("delete from charactergroups where charactergroupid=?",getId());
    }

    /** Returns the type of group for this group.
     * 
     * @return Group type
     */
    public String getType() { return dqs(true,"select type from charactergroups where charactergroupid=?",getId()); }
    
    
    /** Joins a character to this group.
     * Character must not already be in this group, and must not be in a competing group.
     * @param character Character to add
     */
    public void addMember(Char character) {
        // in group?
        if (character.getInstance()!=getInstance()) { throw new SystemException("Character (group) / Instance mismatch"); }
        int exists=dqi(true,"select count(*) from charactergroupmembers where charactergroupid=? and characterid=?",getId(),character.getId());
        if (exists>0) { throw new UserException("Character is already a member of group?"); }
        // in competing group?
        CharacterGroup competition=character.getGroup(this.getType());
        if (competition!=null) { throw new UserException("Unable to join new group, already in a group of type "+this.getType()+" - "+competition.getName()); }
        d("insert into charactergroupmembers(charactergroupid,characterid) values(?,?)",getId(),character.getId());
    }
    /** Remove character from this group
     * 
     * @param character Character to remove
     */
    public void removeMember(Char character) {
        if (character.getInstance()!=getInstance()) { throw new SystemException("Character (group) / Instance mismatch"); }
        int exists=dqi(true,"select count(*) from charactergroupmembers where charactergroupid=? and characterid=?",getId(),character.getId());
        d("delete from charactergroupmembers where charactergroupid=? and characterid=?",getId(),character.getId());
        if (exists==0) { throw new UserException("Character not in group."); }
    }

    /** Get the owner of this group.
     * 
     * @return Character that owns the group
     */
    public Char getOwner() {
        Integer owner=dqi(true,"select owner from charactergroups where charactergroupid=?",getId());
        if (owner==null) { return null; }
        return Char.get(owner);
    }

    /** Set the owner of this group.
     * 
     * @param newowner Character to own the group
     */
    public void setOwner(Char newowner) {
        if (newowner==null) { 
            d("update charactergroups set owner=null where charactergroupid=?",getId());
        } else {
            d("update charactergroups set owner=? where charactergroupid=?",newowner.getId(),getId());
        }
    }

    /** Determines if this character is an admin of the group.
     * Group owner is always an admin
     * @param character Character to check
     * @return true if admin, false otherwise
     */
    public boolean isAdmin(Char character) {
        if (character.getInstance()!=getInstance()) { throw new SystemException("Character (group) / Instance mismatch"); }
        if (this.getOwner()==character) { return true; }
        Integer adminflag=dqi(true,"select isadmin from charactergroupmembers where charactergroupid=? and characterid=?",getId(),character.getId());
        if (adminflag==null) { return false; }
        if (adminflag==1) { return true; }
        return false;
    }
    /** Determine if this states character is an admin of the group
     * 
     * @param state State
     * @return true if admin, else false
     */
    public boolean isAdmin(State st) { return isAdmin(st.getCharacter()); }

    /** Set admin status on a character in this group
     * 
     * @param character Character to update
     * @param admin true/false admin status
     */
    public void setAdmin(Char character, boolean admin) {
        d("update charactergroupmembers set isadmin=? where charactergroupid=? and characterid=?",admin,getId(),character.getId());
    }

    /** Check a character is a member of this group.
     * 
     * @param character Character to search
     * @return true/false
     */
    public boolean hasMember(Char character) {
        Integer count=dqi(true,"select count(*) from charactergroupmembers where charactergroupid=? and characterid=?",getId(),character.getId());
        if (count==null) { throw new SystemException("Null response from count statement"); }
        if (count>1) { throw new SystemException("Matched too many members ("+count+") for "+character+" in CG "+getId()+" - "+getName()); }
        if (count==1) { return true; }
        return false;
    }
    public String getKVTable() { return "charactergroupkvstore"; }
    public String getKVIdField() { return "charactergroupid"; }

    public void validate(State st) throws SystemException {
        if (validated) { return; }
        validate();
        if (st.getInstance()!=getInstance()) { throw new SystemException("CharacterGroup / State Instance mismatch"); }
    }
    
    protected int getNameCacheTime() { return 60; } // character groups are likely to end up renamable

    public Boolean isOpen() {
        return getBool("open");
    }
    
    public void setOpen(Boolean b) { set("open",b); }

    public String getTypeNotNull() {
        String ret=getType();
        if (ret==null) { return ""; }
        return ret;
    }

}
