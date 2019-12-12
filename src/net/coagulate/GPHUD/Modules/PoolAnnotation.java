package net.coagulate.GPHUD.Modules;


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

	public String name() { return meta.name(); }

	public String description() { return meta.description(); }

	public String getName() { return name(); }

	public String fullName() { return module.getName() + "." + getName(); }

}
