package net.coagulate.GPHUD.Modules;

import net.coagulate.GPHUD.State;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;

/** Wraps a KV (Key Value) element.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class KVAnnotation extends KV {
    KVS meta;
    KVAnnotation(){}
    boolean generated=true;
    public boolean isGenerated() { return generated; } 
    Module module;
    public KVAnnotation(Module m,KVS meta) throws UserException, SystemException{this.module=m;this.meta=meta; validate(null); generated=false; }
    public String fullname() { return module.getName()+"."+meta.name(); }
    public String name() { return meta.name(); }
    public KVSCOPE scope() { return meta.scope(); }
    public KVTYPE type() { return meta.type(); }
    public String description() { return meta.description(); }
    public String editpermission() { return meta.editpermission(); }
    public String defaultvalue() { return meta.defaultvalue(); }
    public String conveyas() { return meta.conveyas(); }
    public KVHIERARCHY hierarchy() { return meta.hierarchy(); }
    public boolean template() { return meta.template(); }
    public boolean hidden() { return meta.hidden(); }
    
    
    private void validate(State st) throws UserException, SystemException {
        if (!editpermission().isEmpty()) {
            Modules.validatePermission(st,editpermission());
        }
    }

}
