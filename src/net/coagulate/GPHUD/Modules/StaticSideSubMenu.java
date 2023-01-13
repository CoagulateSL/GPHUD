package net.coagulate.GPHUD.Modules;

/**
 * A simple constructible side sub menu.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class StaticSideSubMenu extends SideSubMenu {
	
	private final String url;
	private final String permission;
	private final int    priority;
	private final String name;
	
	public StaticSideSubMenu(final String name,final int priority,final String url,final String permission) {
		this.name=name;
		this.priority=priority;
		this.url=url;
		this.permission=permission;
	}
	
	
	// ---------- INSTANCE ----------
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
