package net.coagulate.GPHUD.Modules;


/** Wraps a permission.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class PermissionAnnotation extends Permission {
    Permissions meta;
    private boolean generated=true;
    public boolean isGenerated() { return generated; } 
    
    public PermissionAnnotation(Permissions m) { meta=m; generated=false; }
    public String name() { return meta.name(); }
    public String description() { return meta.description(); }
    public boolean grantable() { return meta.grantable(); }
}
