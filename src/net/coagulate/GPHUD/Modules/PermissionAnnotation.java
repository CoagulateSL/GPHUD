package net.coagulate.GPHUD.Modules;


import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * Wraps a permission.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class PermissionAnnotation extends Permission {
	final Permissions meta;
	private final boolean generated;
	@Nonnull
	private final String modulename;

	public PermissionAnnotation(final Permissions m,
	                            @Nonnull final String modulename) {
		meta=m;
		generated=false;
		this.modulename=modulename;
	}

	// ---------- INSTANCE ----------
	@Override
	public Module getModule(final State st) {
		return Modules.get(st,modulename);
	}

	public boolean isGenerated() { return generated; }

	@Nonnull
	public String name() { return meta.name(); }

	@Nonnull
	public String description() { return meta.description(); }

	@Nonnull
	@Override
	public POWER power() {
		return meta.power();
	}

	public boolean grantable() { return meta.grantable(); }
}
