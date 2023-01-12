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
	
	public KVEnabled(final Module m,final String def) {
		module=m;
		this.def=def;
	} // technically is generated, but generated really means "instance specific", and these KVs aren't, they're just a convenience for writing the static declaration.
	
	// ---------- INSTANCE ----------
	@Nonnull
	public String name() {
		return "Enabled";
	}
	
	@Override
	public boolean isGenerated() {
		return false;
	}
	
	@Nonnull
	@Override
	public String fullName() {
		return module.getName()+"."+name();
	}
	
	@Nonnull
	public KVSCOPE scope() {
		return KVSCOPE.INSTANCE;
	}
	
	@Nonnull
	public KVTYPE type() {
		return KVTYPE.BOOLEAN;
	}
	
	@Nonnull
	public String description() {
		return "Enabled flag for this module";
	}
	
	@Nonnull
	public String editPermission() {
		return "Instance.ModuleEnablement";
	}
	
	public String defaultValue() {
		return def;
	}
	
	@Nonnull
	public String conveyAs() {
		return "";
	}
	
	@Nonnull
	public KVHIERARCHY hierarchy() {
		return KVHIERARCHY.NONE;
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
