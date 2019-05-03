package net.coagulate.GPHUD.Modules;


/**
 * Wraps a side menu.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class SideMenuAnnotation extends SideMenu {
	SideMenus meta;
	private boolean generated = true;

	public SideMenuAnnotation(SideMenus meta) {
		generated = false;
		this.meta = meta;
	}

	public boolean isGenerated() { return generated; }

	public String name() { return meta.name(); }

	public int priority() { return meta.priority(); }

	public String url() { return meta.url(); }

	public String requiresPermission() { return meta.requiresPermission(); }
}
