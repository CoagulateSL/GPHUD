package net.coagulate.GPHUD.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import static java.util.logging.Level.SEVERE;
import net.coagulate.Core.Database.DBException;
import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Database.Results;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import static net.coagulate.Core.Tools.UnixTime.getUnixTime;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;

/** Reference to an avatar, independant of an instance.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Avatar extends TableRow {
    
    /** Factory style constructor
     * 
     * @param id the ID number we want to get
     * @return An Avatar representation
     */
    public static Avatar get(int id) {
        return (Avatar)factoryPut("Avatar",id,new Avatar(id));
    }

    /** Obtain a reference to the SYSTEM avatar, for auditing system functions.
     * 
     * @param st State
     * @return Reference to the SYSTEM avatar
     */
    public static Avatar getSystem(State st) {
        return findOrCreateAvatar(st, "SYSTEM", "DEADBEEF");
    }

    /** Get the set of avatars owned by a user.
     * 
     * @param id 
     * @return 
     */
    static Set<Avatar> getByOwner(User id) {
        Set<Avatar> avatars=new TreeSet<>();
        for (ResultsRow r:GPHUD.getDB().dq("select avatarid from avatars where owner=?",id.getId())) {
            avatars.add(Avatar.get(r.getInt()));
        }
        return avatars;
    }

    protected Avatar(int id) { super(id); }
    /** Find avatar in database, or create record if necessary.
     * 
     * @param name Name of avatar
     * @param st State
     * @param key UUID of the avatar
     * @return Avatar object
     */
    public static Avatar findOrCreateAvatar(State st,String name,String key) throws SystemException {
        if (name==null || name.equals("")) { name=""; }
        Integer avatarid=GPHUD.getDB().dqi(false,"select avatarid from avatars where (avatarname=? or avatarkey=?)",name,key);
        if (avatarid==null) {
            if (name.contains("Loading...")) { throw new SystemException("No avatar name was sent with the key, can not create new avatar record"); }
            try {
                // special key used by the SYSTEM avatar
                if (!key.equals("DEADBEEF")) {
                    State fake=new State();
                    if (st.getInstanceNullable()!=null) { fake.setInstance(st.getInstance()); }
                    fake.setAvatar(getSystem(st));
                    st.logger().info("Creating new avatar entry for '"+name+"'");
                }
                GPHUD.getDB().d("insert into avatars(avatarname,lastactive,avatarkey) values(?,?,?)",name,getUnixTime(),key);
            } catch (DBException ex) {
                st.logger().log(SEVERE,"Exception creating avatar "+name,ex);
                throw ex;
            }
            avatarid=GPHUD.getDB().dqi(false,"select avatarid from avatars where owner is null and (avatarname=? or avatarkey=?)",name,key);            
        }
        if (avatarid==null) {
            st.logger().severe("Failed to find avatar '"+name+"' after creating it");
            throw new NoDataException("Failed to find avatar object for name '"+name+"' after we created it!");
        }
        return get(avatarid); 
    }
    
     /** Find avatar in database, by name or key.
     * 
     * @param name Name or UUID of avatar
     * @return Avatar object
     */
    public static Avatar find(String nameorkey) {
        if (nameorkey==null || nameorkey.equals("")) { throw new UserException("Avatar name not supplied"); }
        Integer avatarid=GPHUD.getDB().dqi(false,"select avatarid from avatars where avatarname=? or avatarkey=?",nameorkey,nameorkey);
        if (avatarid==null) {
            throw new UserException("Failed to find avatar object for name or key '"+nameorkey+"'");
        }
        return get(avatarid); 
    }
    /** Get the owner of this avatar.
     * 
     * @return User object that owns this avatar, or (more likely) null
     */
    public User getOwner() {
        Integer i=getInt("owner");
        if (i==null) { return null; }
        return User.get(i);
    }

    private Char getPrimaryCharacter_internal(Instance instance) {
        Integer primary=dqi(true,"select entityid from primarycharacters where avatarid=? and instanceid=?",getId(),instance.getId());
        Char c=Char.get(primary);
        if (c.retired()) {
            d("delete from primarycharacters where avatarid=? and instanceid=?",getId(),instance.getId());
            throw new NoDataException("Primary character is retired");
        }
        return c;
    }
    /** Get a primary character for this avatar in a particular instance, or create one.
     * 
     * @param st Session state containing instance.
     * @return
     */
    public Char getPrimaryCharacter(State st,boolean autocreate) {
        Instance instance=st.getInstance();
        try {
            return getPrimaryCharacter_internal(instance);
        }
        catch (NoDataException e) {
            // hmm, well, lets make them one then.
            Set<Char> characterset=getCharacters(instance);
            if (characterset.isEmpty()) {
                if (!autocreate) { return null; }
                // make them a character
                st.logger().info("Created default character for "+toString());
                create(st,getName());
                characterset=getCharacters(instance);
                if (characterset.isEmpty()) {
                    st.logger().severe("Created character for avatar but avatar has no characters still");
                    throw new NoDataException("Could not create a character for this avatar");
                }
                Audit.audit(st,Audit.OPERATOR.AVATAR,st.user, this, Char.get(characterset.iterator().next().getId()), "Create", "Character", null, getName(), "Automatically generated character upon login with no characters.");
            }
            d("insert into primarycharacters(avatarid,instanceid,entityid) values(?,?,?)",getId(),instance.getId(),characterset.iterator().next().getId());
            return getPrimaryCharacter_internal(instance);
        }
    }

    public void setPrimaryCharacter(State st,Char c) {
        c.validate(st);
        d("delete from primarycharacters where avatarid=? and instanceid=?",st.avatar().getId(),st.getInstance().getId());
        d("insert into primarycharacters(avatarid,instanceid,entityid) values(?,?,?)",st.avatar().getId(),st.getInstance().getId(),c.getId());
    }
    
    public void create(State st,String name) {
        d("insert into characters(name,instanceid,owner,lastactive,retired) values(?,?,?,?,?)",name,st.getInstance().getId(),getId(),getUnixTime(),0);
    }
    
    @Override
    public String getTableName() {
        return "avatars";
    }

    @Override
    public String getIdField() {
        return "avatarid";
    }

    @Override
    public String getNameField() {
        return "avatarname";
    }

    /** Get list of users characters at a given instance.
     * 
     * @param instance The instance
     * @return List of Char (characters)
     */
    public Set<Char> getCharacters(Instance instance) {
        Results rows=dq("select characterid from characters where owner=? and retired=0 and instanceid=?",getId(),instance.getId());
        Set<Char> results=new TreeSet<>();
        for (ResultsRow r:rows) {
            results.add(Char.get(r.getInt()));
        }
        return results;
    }
    
    /** Get all characters for this avatar, at any instance.
     * 
     * @return  Set of Char that are owned by this avatar and not retired
     */
    public Set<Char> getCharacters() {
        Results rows=dq("select characterid from characters where owner=? and retired=0",getId());
        Set<Char> results=new TreeSet<>();
        for (ResultsRow r:rows) {
            results.add(Char.get(r.getInt()));
        }
        return results;
    }

    /** Gets all the permissions a user has at an instance.
     * @param instance
     * @return Set of permissions.
     */
    public Set<String> getPermissions(Instance i) {
        Set<String> permissions=new TreeSet<>();
        Results results=dq("select permission from permissions,permissionsgroups,permissionsgroupmembers where permissions.permissionsgroupid=permissionsgroups.permissionsgroupid and instanceid=? and permissionsgroupmembers.permissionsgroupid=permissionsgroups.permissionsgroupid and permissionsgroupmembers.avatarid=?",i.getId(),getId());
        for (ResultsRow r:results) {
            permissions.add(r.getString());
        }
        return permissions;
    }

    @Override
    public String getLinkTarget() {
        return "avatars";
    }

    /** Start a visit record.
     * Ends any existing visit records for this avatar.
     * @param st State
     * @param character character that is visiting
     * @param region region that is being visited
     */
    public void initVisit(State st,Char character, Region region) {
        int updates=dqi(true,"select count(*) from visits where avatarid=? and endtime is null",getId());
        if (updates>0) {
            st.logger().fine("Force terminating "+updates+" visits");
            d("update visits set endtime=? where avatarid=? and endtime is null",getUnixTime(),getId());
            Set<Avatar> avatarset=new HashSet<>();
            avatarset.add(this);
            for (Region reg:st.getInstance().getRegions()) { reg.departingAvatars(st,avatarset); }
        }
        st.logger().fine("Starting visit for "+character.getNameSafe()+" at "+region.getNameSafe()+" on avatar "+getNameSafe());
        d("insert into visits(avatarid,characterid,regionid,starttime) values(?,?,?,?)",getId(),character.getId(),region.getId(),getUnixTime());
    }
    
    /** Update the lastactive timer for the avatar.
     * 
     */
    public void visit() {
        d("update avatars set lastactive=? where avatarid=?",getUnixTime(),getId());
    }

    /** Get the currently active character for this avatar.
     * 
     * @return Active character
     */
    public Char getActiveCharacter() {
        return Char.getActive(this);
    }

    /** Gets the avatar's last active timestamp.
     * 
     * @return The last active time for an avatar, possibly null.
     */
    public Integer getLastActive() {
        return dqi(true,"select lastactive from avatars where avatarid=?",getId());
    }

    /** Sets the last used instance for the avatar - used for logging in to the web portal.
     * 
     * @param i  Instance to set to
     */
    public void setLastInstance(Instance i) {
        d("update avatars set lastinstance=? where avatarid=?",i.getId(),getId());
    }

    /** Gets the last used instance, used as the default instance on web portal login.
     * 
     * @return Last used instance, or null if no last used instance.
     */
    public Instance getLastInstance() {
        Integer newid=getInt("lastinstance");
        if (newid==null) { return null; }
        return Instance.get(newid);
    }

    /** Set the owner of this avatar.
     * 
     * @param user Owner of this avatar
     */
    public void setOwner(User user) {
        set("owner",user.getId());
    }
    
    // actually avatars do have a KV store but no functional need has yet been found.
    // until such time, it is disabled by returning null to the KV query methods, which will be trapped if the KV map is accessed.
    
    public String getKVTable() { return null; }
    public String getKVIdField() { return null; }
    public void flushKVCache(State st) {}

    /** Return the avatar's UUID.
     * 
     * @return UUID (Avatar Key)
     */
    public String getUUID() {
        return getString("avatarkey");
    }

    public void validate(State st) throws SystemException {
        if (validated) { return; }
        validate();
    }

    protected int getNameCacheTime() { return 60*60; } // this name doesn't change, cache 1 hour
    
    public String getTimeZone() {
        String s=getString("timezone");
        if (s==null) { return "America/Los_Angeles"; }
        if (s.equals("SLT")) { return "America/Los_Angeles"; }
        return s;
    }
    
    public void setTimeZone(String timezone) { set("timezone",timezone); }

    public boolean canCreate() {
        return getBool("cancreate");
    }

    public void canCreate(boolean cancreate) {
        set("cancreate",cancreate);
    }

    /** Find character by name
     * 
     * @param st State
     * @param name Character name
     * @return Character
     */
    public static Avatar resolve(String name) {
        int id=GPHUD.getDB().dqi(true,"select avatarid from avatars where avatarname like ?",name);
        if (id==0) { return null; }
        return get(id);
    }

    public Set<Char> getPlayableCharacters(Instance instance) {
        Set<Char> characters = getCharacters(instance);
        Set<Char> ret=new HashSet<>();
        for (Char c:characters) {
            ret.add(c); //TODO check char isn't retired
        }
        return ret;
    }


}
