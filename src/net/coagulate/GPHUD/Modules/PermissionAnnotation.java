package net.coagulate.GPHUD.Modules;


import net.coagulate.GPHUD.State;

/**
 * Wraps a permission.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class PermissionAnnotation extends Permission {
	final Permissions meta;
	private boolean generated = true;
	private String modulename=null;

	public PermissionAnnotation(Permissions m,String modulename) {
		meta = m;
		generated = false;
		this.modulename=modulename;
	}

	@Override
	public Module getModule(State st) {
		return Modules.get(st,modulename);
	}

	public boolean isGenerated() { return generated; }

	public String name() { return meta.name(); }

	public String description() { return meta.description(); }

	@Override
	public POWER power() {
		return meta.power();
	}

	public boolean grantable() { return meta.grantable(); }
}
