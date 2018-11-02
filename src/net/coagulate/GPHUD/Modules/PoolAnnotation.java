package net.coagulate.GPHUD.Modules;


/** Wraps a Pool.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class PoolAnnotation extends Pool {
    Pools meta;
    Module module;
    private boolean generated=true;
    public boolean isGenerated() { return generated; } 
    

    public PoolAnnotation(Module mod,Pools meta) { this.module=mod; this.meta=meta; generated=false; }
    public String name() { return meta.name(); }
    public String description() { return meta.description(); }
    public String getName() { return name(); }
    public String fullName() { return module.getName()+"."+getName(); }
    
}
