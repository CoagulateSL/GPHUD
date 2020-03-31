package net.coagulate.GPHUD.Modules;


import javax.annotation.Nonnull;

/**
 * Wraps a Pool.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class PoolAnnotation extends Pool {
	final Pools meta;
	final Module module;
	private final boolean generated;

	public PoolAnnotation(final Module mod,
	                      final Pools meta) {
		module=mod;
		this.meta=meta;
		generated=false;
	}

	// ---------- INSTANCE ----------
	public boolean isGenerated() { return generated; }

	@Nonnull
	public String description() { return meta.description(); }

	@Nonnull
	public String fullName() { return module.getName()+"."+getName(); }

	@Nonnull
	public String name() { return meta.name(); }

	@Nonnull
	public String getName() { return name(); }

}
