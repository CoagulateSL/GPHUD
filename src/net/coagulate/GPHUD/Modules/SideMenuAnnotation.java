package net.coagulate.GPHUD.Modules;


import javax.annotation.Nonnull;

/**
 * Wraps a side menu.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class SideMenuAnnotation extends SideMenu {
	final         SideMenus meta;
	private final boolean   generated;
	
	public SideMenuAnnotation(final SideMenus meta) {
		generated=false;
		this.meta=meta;
	}
	
	// ---------- INSTANCE ----------
	public boolean isGenerated() {
		return generated;
	}
	
	@Nonnull
	public String name() {
		return meta.name();
	}
	
	public int priority() {
		return meta.priority();
	}
	
	@Nonnull
	public String url() {
		return meta.url();
	}
	
	@Nonnull
	public String requiresPermission() {
		return meta.requiresPermission();
	}
}
