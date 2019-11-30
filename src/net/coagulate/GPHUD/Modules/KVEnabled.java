package net.coagulate.GPHUD.Modules;

import javax.annotation.Nonnull;

/**
 * Hard wired "enabled" flag for modules.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class KVEnabled extends KV {

	final Module module;
	final String def;

	public KVEnabled(Module m, String def) {
		this.module = m;
		this.def = def;
	} // technically is generated, but generated really means "instance specific", and these KVs aren't, they're just a convenience for writing the static declaration.

	@Nonnull
	public String name() { return "Enabled"; }

	@Nonnull
	public KVSCOPE scope() { return KVSCOPE.INSTANCE; }

	@Nonnull
	public KVTYPE type() { return KVTYPE.BOOLEAN; }

	@Nonnull
	public KVHIERARCHY hierarchy() { return KVHIERARCHY.NONE; }

	@Nonnull
	public String description() { return "Enabled flag for this module"; }

	@Nonnull
	public String editpermission() { return "Instance.ModuleEnablement"; }

	public String defaultvalue() { return def; }

	@Nonnull
	public String conveyas() { return ""; }

	@Override
	public boolean isGenerated() {
		return false;
	}

	@Nonnull
	@Override
	public String fullname() {
		return module.getName() + "." + name();
	}

	@Override
	public boolean template() {
		return false;
	}

	@Override
	public boolean hidden() {
		return true;
	}


}
