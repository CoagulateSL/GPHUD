package net.coagulate.GPHUD.Modules;

/**
 * Hard wired "enabled" flag for modules.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class KVEnabled extends KV {

	Module module;
	String def;

	public KVEnabled(Module m, String def) {
		this.module = m;
		this.def = def;
	} // technically is generated, but generated really means "instance specific", and these KVs aren't, they're just a convenience for writing the static declaration.

	public String name() { return "Enabled"; }

	public KVSCOPE scope() { return KVSCOPE.INSTANCE; }

	public KVTYPE type() { return KVTYPE.BOOLEAN; }

	public KVHIERARCHY hierarchy() { return KVHIERARCHY.NONE; }

	public String description() { return "Enabled flag for this module"; }

	public String editpermission() { return "Instance.ModuleEnablement"; }

	public String defaultvalue() { return def; }

	public String conveyas() { return ""; }

	@Override
	public boolean isGenerated() {
		return false;
	}

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
