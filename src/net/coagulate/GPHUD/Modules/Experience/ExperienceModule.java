package net.coagulate.GPHUD.Modules.Experience;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Attribute;
import static net.coagulate.GPHUD.Data.Attribute.ATTRIBUTETYPE.EXPERIENCE;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.CHARACTER;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.INTEGER;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.TEXT_INTERNAL_NAME;
import static net.coagulate.GPHUD.Modules.Argument.ArgumentType.TEXT_ONELINE;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Characters.CharacterAttribute;
import net.coagulate.GPHUD.Modules.Command.Commands;
import static net.coagulate.GPHUD.Modules.Command.Context.AVATAR;
import net.coagulate.GPHUD.Modules.KV;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Permission;
import net.coagulate.GPHUD.Modules.Pool;
import net.coagulate.GPHUD.State;

/** Dynamic events menu
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ExperienceModule extends ModuleAnnotation {

    @Override
    public void validatePermission(State st, String permission) {
        return; // really can't validate these as they can be dynamic
    }

    @Override
    public Map<String, Permission> getPermissions(State st) {
        Map<String,Permission> perms=super.getPermissions(st);
        for (Attribute a:st.getAttributes()) {
            if (a.getType()==EXPERIENCE) {
                perms.put("award"+a.getName()+"XP", new GenericXPAwardPermission(a.getName()));
            }
        }
        return perms;
    }

    @Override
    public Permission getPermission(State st, String itemname) {
        Map<String,Permission> perms=getPermissions(st);
        for (String s:perms.keySet()) {
            if (s.equalsIgnoreCase(itemname)) {
                return perms.get(s);
            }
        }
        return super.getPermission(st,itemname);
    }

    @Override
    public Map<String, Pool> getPoolMap(State st) {
        Map<String,Pool> pools=new HashMap<>();
        pools.putAll(poolmap);
        if (st!=null) {
            if (st.getInstanceNullable()==null) { return pools; }
            for (Attribute attr:st.getAttributes()) {
                if (attr.getType()==EXPERIENCE) {
                    pools.put(attr.getName()+"XP", new GenericXPPool(attr.getName()));
                }
            }
        }
        return pools;
    }

    @Override
    public Pool getPool(State st, String itemname) {
        Map<String,Pool> pmap=getPoolMap(st);
        for (String name:pmap.keySet()) {
            if (name.equalsIgnoreCase(itemname)) { return pmap.get(name); }
        }
        throw new SystemException("Unable to retrieve pool "+itemname);
    }

    @Override
    public KV getKVDefinition(State st, String qualifiedname) throws SystemException {
        if (kvmap.containsKey(qualifiedname.toLowerCase())) {
            return kvmap.get(qualifiedname.toLowerCase());
        }
        Map<String,KV> map=getKVDefinitions(st);
        for (String s:map.keySet()) { if ((s).equalsIgnoreCase(qualifiedname)) { return map.get(s); } }
        throw new SystemException("Invalid KV "+qualifiedname+" in module "+getName());
    }

    @Override
    public Map<String, KV> getKVDefinitions(State st) {
        Map<String,KV> map=new TreeMap<>();
        map.putAll(kvmap);
        if (st.getInstanceNullable()==null) { return map; }
        for (Attribute attr:st.getAttributes()) {
            if (attr.getType()==EXPERIENCE) {
                map.put(attr.getName()+"XPPeriod", new GenericXPPeriodKV(attr.getName()+"XPPeriod"));
                map.put(attr.getName()+"XPLimit", new GenericXPLimitKV(attr.getName()+"XPLimit"));
            }
        }     
        return map;
    }
    
    public ExperienceModule(String name, ModuleDefinition def) throws SystemException, UserException {
        super(name, def);
    }

    @Override
    public Set<CharacterAttribute> getAttributes(State st) {
        Set<CharacterAttribute> ret=new HashSet<>();
        if (st.hasModule("Events")) { ret.add(new EventXP(-1)); }
        ret.add(new VisitXP(-1));
        if (st.hasModule("Faction")) { ret.add(new FactionXP(-1)); }
        for (Attribute attr:st.getAttributes()) {
            if (attr.getType()==EXPERIENCE) {
                ret.add(new GenericXP(attr.getName()+"XP"));
            }
        }
        return ret;
    }
    
    
    @Commands(context = AVATAR,requiresPermission = "",description = "Award XP")
    public static Response award(State st,
        @Arguments(description="Character to award to",mandatory=true,type = CHARACTER)
            Char target,
        @Arguments(description = "XP type to award",mandatory = true,type=TEXT_INTERNAL_NAME,max=32)
            String type,
        @Arguments(description="Ammount to award",mandatory=false,type=INTEGER,max=999999)
            Integer ammount,
        @Arguments(description="Reason for award",mandatory=false,type=TEXT_ONELINE,max=128)
            String reason
            
        ){
        if (ammount==null) { ammount=1; }
        Attribute attr=st.getAttribute(type);
        if (!st.hasPermission("Experience.award"+attr.getName()+"XP")) {
            return new ErrorResponse("You require permission Experience.award"+attr.getName()+"XP to execute this command");
        }
        if (attr.getType()!=EXPERIENCE) {
            return new ErrorResponse("This attributes is not of type EXPERIENCE, (try omitting XP off the end, if you have this)");
        }
        Pool p=Modules.getPool(st,"Experience."+type+"XP");
        GenericXPPool gen=(GenericXPPool)p;
        gen.awardXP(st, target, reason,ammount);
        return new OKResponse("Awarded "+ammount+" "+p.name()+" to "+target.getName());
    }

}
