package net.coagulate.GPHUD.Modules;


import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Wraps a permission.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class PermissionAnnotation extends Permission {
	final Permissions meta;
	private boolean generated = true;
	@Nullable
	private String modulename=null;

	public PermissionAnnotation(Permissions m, @Nullable String modulename) {
		meta = m;
		generated = false;
		this.modulename=modulename;
	}

	@Override
	public Module getModule(State st) {
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
