package net.coagulate.GPHUD.Modules;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import static java.util.logging.Level.FINE;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.CharacterGroup;
import net.coagulate.GPHUD.Data.Event;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Data.TableRow;
import net.coagulate.GPHUD.Data.Zone;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

/** Wraps a KV (Key Value) element.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class KV extends NameComparable {

    /** Defines a KVS element.
     * @param name Name of the permission within your modules namespace (abc becomes module.abc)
     * @param description Description of the permission
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Target(ElementType.PACKAGE)
    @Repeatable(KVSS.class)
    public @interface KVS {
        String name();
        KVSCOPE scope();
        KVTYPE type();
        String description();
        String editpermission();
        String defaultvalue();
        String conveyas() default "";
        KVHIERARCHY hierarchy() default KVHIERARCHY.NONE;
        boolean template();
        boolean hidden() default false;
    }
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Target(ElementType.PACKAGE)
    public @interface KVSS {
        KVS[] value();
    }    

    public static enum KVSCOPE {INSTANCE,SERVER,SPATIAL,CHARACTER,ZONE, EVENT,COMPLETE};
    public static enum KVTYPE { TEXT,INTEGER,FLOAT,UUID,BOOLEAN,COMMAND,COLOR};
    
    // Configurable THINGS
    // Characters, Events, Zones, Regions, Instances, Avatars (To be removed?), CharacterGroups (to be added)
    public static enum KVHIERARCHY { NONE, DELEGATING, AUTHORITATIVE, CUMULATIVE};
            
    public abstract boolean isGenerated();
    public abstract String fullname();
    public abstract KVSCOPE scope();
    public abstract KVTYPE type();
    public abstract String description();
    public abstract String editpermission();
    public abstract String defaultvalue();
    public abstract String conveyas();
    public abstract KVHIERARCHY hierarchy();
    public abstract boolean template();
    public boolean hidden() { return false; }
  
    public boolean appliesTo(TableRow o) {
        switch (scope()) {
            case CHARACTER:
                if (o instanceof Char) { return true; }
                return false;
            case EVENT:
                if (o instanceof Event) { return true; }
                return false;
            case INSTANCE:
                if (o instanceof Instance) { return true; }
                return false;
            case SERVER:
                if (o instanceof Instance ||
                        o instanceof Region) { return true; }
                return false;
            case SPATIAL:
                if (o instanceof Instance ||
                        o instanceof Region ||
                        o instanceof Zone ||
                        o instanceof Event) { return true; }
                return false;
            case COMPLETE:
                // probably should just return true but...
                if (    o instanceof Instance ||
                        o instanceof Region ||
                        o instanceof Zone ||
                        o instanceof Event ||
                        o instanceof CharacterGroup ||
                        o instanceof Char) { return true; }
                throw new SystemException("KV of COMPLETE scope was passed unknown DBObject "+o.getClass().getName()+".  Ammend whitelist or debug caller.");
            case ZONE:
                if (o instanceof Zone) { return true; }
                return false;
            default:
                throw new SystemException("Unhandled scope "+scope());
        }
    }
    /* DEAD CODE?
    public  boolean exclusiveTo(DBObject o) {
         switch (scope()) {
            case CHARACTER:
                if (o instanceof Char) { return true; }
                return false;
            case EVENT:
                if (o instanceof Event) { return true; }
                return false;
            case INSTANCE:
                if (o instanceof Instance) { return true; }
                return false;
            case SERVER: return false;
            case SPATIAL:
                return false;
            case COMPLETE:
                return false;
            case ZONE:
                if (o instanceof Zone) { return true; }
                return false;
            default:
                throw new SystemException("Unhandled scope "+scope());
        }
    }
    */

    
    public void assertAppliesTo(TableRow o) { if (!appliesTo(o)) { throw new UserException("Can not apply scope "+scope()+" to "+name()); } }
    
    public void setKV(State st,TableRow o,String value) throws UserException, SystemException {
        assertAppliesTo(o);
        if (!(TableRow.class.isAssignableFrom(o.getClass()))) { throw new SystemException("Object "+o.getClass()+" does not extend DBOBject while setting KV "+name()); }
        TableRow applyto=(TableRow)o;
        st.setKV(o, name(), value);
        convey(st,value);
    }
    
    public void convey(State st,String value) throws UserException, SystemException {
        if (!conveyas().isEmpty()) {
            Set<Region> regions=st.getInstance().getRegions();
            JSONObject message=new JSONObject();
            message.put("incommand", "broadcast");
            message.put(conveyas(),value);
            for (Region r:regions) {
                st.logger().log(FINE,"Conveying to "+r+" '"+name()+"' "+conveyas()+"="+value);
                Transmission t=new Transmission(r,message);
                t.start();
            }
        }
    }
}
