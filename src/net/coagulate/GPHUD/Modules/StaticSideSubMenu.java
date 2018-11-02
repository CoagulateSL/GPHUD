package net.coagulate.GPHUD.Modules;

/** A simple constructible side sub menu.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class StaticSideSubMenu extends SideSubMenu {

    private String url;
    private String permission;
    private int priority;
    private String name;

    public StaticSideSubMenu(String name,int priority,String url,String permission) { 
        this.name=name;
        this.priority=priority;
        this.url=url;
        this.permission=permission;
    }
    
    
    
    @Override
    public String name() {
        return name;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public String requiresPermission() {
        return permission;
    }

    @Override
    public boolean isGenerated() {
        return true;
    }

    @Override
    public String getURL() {
        return url;
    }
    
}
