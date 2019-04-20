package net.coagulate.GPHUD.Data;

import java.util.Map;
import java.util.TreeMap;
import net.coagulate.Core.Database.ResultsRow;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

/** Alias entry.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Alias extends TableRow {
    
    /** Factory style constructor
     * 
     * @param id the ID number we want to get
     * @return An Avatar representation
     */
    public static Alias get(int id) { return (Alias)factoryPut("Alias",id,new Alias(id)); }
    
    /** Returns a map of aliases for this state.
     *
     * @param st State
     * @return Map of Alias Name to Alias objects
     */
    public static Map<String, Alias> getAliasMap(State st) {
        Map<String,Alias> aliases= new TreeMap<>();
        for (ResultsRow r:GPHUD.getDB().dq("select name,aliasid from aliases where instanceid=?",st.getInstance().getId())) {
            aliases.put(r.getString("name"),get(r.getInt("aliasid")));
        }
        return aliases;
    }

    /** Get aliased command templates for this state
     * 
     * @param st State
     * @return Map of Name to Template (JSON) mappings
     */
    public static Map<String, JSONObject> getTemplates(State st) {
        Map<String,JSONObject> aliases= new TreeMap<>();
        for (ResultsRow r:GPHUD.getDB().dq("select name,template from aliases where instanceid=?",st.getInstance().getId())) {
            aliases.put(r.getString("name"),new JSONObject(r.getString("template")));
        }
        return aliases;        
    }

    protected Alias(int id) { super(id); }

    @Override
    public String getTableName() {
        return "aliases";
    }

    @Override
    public String getIdField() {
        return "aliasid";
    }

    @Override
    public String getNameField() {
        return "name";
    }

    public Instance getInstance() {
        return Instance.get(getInt("instanceid"));
    }
    
    public static Alias getAlias(State st,String name) {
        Integer id=GPHUD.getDB().dqi(false,"select aliasid from aliases where instanceid=? and name like ?",st.getInstance().getId(),name);
        if (id==null) { return null; }
        return get(id);
    }

    @Override
    public String getLinkTarget() {
           return "/configuration/aliases/view/"+getId();
    }
    
    public static Alias create(State st,String name,JSONObject template) throws UserException, SystemException {
        if (getAlias(st,name)!=null) { throw new UserException("Alias "+name+" already exists"); }
        if (name.matches(".*[^A-Za-z0-9-=_,].*")) { throw new UserException("Aliases must not contain spaces, and mostly only allow A-Z a-z 0-9 - + _ ,"); } 
        GPHUD.getDB().d("insert into aliases(instanceid,name,template) values(?,?,?)",st.getInstance().getId(),name,template.toString());
        Alias newalias=getAlias(st,name);
        if (newalias==null) { throw new SystemException("Failed to create alias "+name+" in instance id "+st.getInstance().getId()+", created but not found?"); }
        return newalias;
    }

    public JSONObject getTemplate() throws SystemException {
        String json=dqs(true,"select template from aliases where aliasid=?",getId());
        JSONObject jsonobject=new JSONObject(json);
        return jsonobject;
    }

    public void setTemplate(JSONObject template) {
        d("update aliases set template=? where aliasid=?",template.toString(),getId());
    }

    public String getKVTable() { return null; }
    public String getKVIdField() { return null; }
    public void flushKVCache(State st) {}

    public void validate(State st) throws SystemException {
        if (validated) { return; }
        validate();
        if (st.getInstance()!=getInstance()) { throw new SystemException("Alias / State Instance mismatch"); }
    }

    protected int getNameCacheTime() { return 60*60; } // this name doesn't change, cache 1 hour
    // for integrity reasons, renames should be doen through recreates (sadface)

    public void delete() {
        d("delete from aliases where aliasid=?",getId());
    }

}
