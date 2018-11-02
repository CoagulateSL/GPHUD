package net.coagulate.GPHUD.Data;

import java.util.Set;
import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;

/** Reference to a User (a global login)
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class User extends TableRow {
    net.coagulate.SL.Data.User wrapped;
    /** Get all the avatars this User owns.
     * 
     * @return Set of Avatars
     */
    public Set<Avatar> getAvatars() {
        return Avatar.getByOwner(this);
    }

    @Override
    public String getLinkTarget() { return "users"; }    

    /** Factory style constructor
     * 
     * @param id the ID number we want to get
     * @return A User representation
     */
    public static User get(int id) {
        return (User)factoryPut("User",id,new User(id));
    }
    
    /** Find a user object by username.
     * 
     * @param username Username to search for
     * @return User object, or null if user does not exist.
     */
    public static User find(String username) {
        try {
            net.coagulate.SL.Data.User towrap = net.coagulate.SL.Data.User.get(username, false);            
            return get(towrap.getId());
        }
        catch (NoDataException e) // not interesting 
        { GPHUD.getLogger().info("Unknown username '"+username+"'"); return null; }
    }
    
    protected User(int id) { super(id); wrapped=net.coagulate.SL.Data.User.get(id); }

    /** Look up a developer key
     * 
     * @param key Developer key to search
     * @return User associated with developer key, or null if not found.
     */
    public static User resolveDeveloperKey(String key) {
        if (key==null || key.equals("")) {
            GPHUD.getLogger().severe("Unable to resolve null or blank developer key"); return null;
        }
        try {
            net.coagulate.SL.Data.User towrap = net.coagulate.SL.Data.User.getDeveloperKey(key);
            return get(towrap.getId());
        } catch (NoDataException e) {
            return null;
        }
    }

    @Override
    public String getTableName() {
        throw new UnsupportedOperationException("Can not comply!");
    }

    @Override
    public String getIdField() {
        throw new UnsupportedOperationException("Can not comply!");
    }

    @Override
    public String getNameField() {
        throw new UnsupportedOperationException("Can not comply!");
    }

    /** Is this user a superadmin?
     * 
     * @return True if so
     */
    public boolean isSuperAdmin() {
        return wrapped.isSuperAdmin();
    }

    /** Get the developer's key
     * 
     * @return Developer key
     */
    private String getDeveloperKey() { 
        return wrapped.getDeveloperKey();
    }
    
    /** Determine if this user has a developer key.
     * 
     * @return True if they have one
     */
    public boolean hasDeveloperKey() {
        String key=getDeveloperKey();
        if (key==null || key.isEmpty()) { return false; }
        return true;
    }
    
    /** Set a user's password
     * 
     * @param newpassword New password
     * @throws SystemException Crypto error or other internal error
     * @throws UserException Password violates policy.
     */
    public void setPassword(String newpassword) throws SystemException, UserException {
        wrapped.setPassword(newpassword);
    }
    
    /** Confirm the user's password
     * 
     * @param password Password to compare to
     * @return True if the supplied password matches, otherwise false.
     * @throws SystemException Internal error such as crypto failure
     */
    public boolean verifyPassword(String password) throws SystemException {
        return wrapped.checkPassword(password);
    }

    public String getKVTable() { return null; }
    public String getKVIdField() { return null; }
    public void flushKVCache(State st) {}

    public void validate(State st) throws SystemException {
        if (validated) { return; }
        validate();
    }
    protected int getNameCacheTime() { return 60*60; } // this name doesn't change, cache 1 hour
    
    @Override
    public String getName() { return wrapped.getUsername(); }
}
