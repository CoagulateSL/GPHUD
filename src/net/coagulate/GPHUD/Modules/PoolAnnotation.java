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
	private boolean generated = true;

	public PoolAnnotation(Module mod, Pools meta) {
		this.module = mod;
		this.meta = meta;
		generated = false;
	}

	public boolean isGenerated() { return generated; }

	@Nonnull
	public String name() { return meta.name(); }

	@Nonnull
	public String description() { return meta.description(); }

	@Nonnull
	public String getName() { return name(); }

	@Nonnull
	public String fullName() { return module.getName() + "." + getName(); }

}
