package net.coagulate.GPHUD.Modules;

import net.coagulate.GPHUD.State;
import net.coagulate.SL.Config;

import javax.annotation.Nonnull;

/**
 * Wraps a KV (Key Value) element.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class KVAnnotation extends KV {
	KVS     meta;
	boolean generated=true;
	Module  module;
	
	KVAnnotation() {
	}
	
	public KVAnnotation(final Module m,final KVS meta) {
		module=m;
		this.meta=meta;
		validate(null);
		generated=false;
	}
	
	// ----- Internal Instance -----
	private void validate(final State st) {
		if (!editPermission().isEmpty()) {
			Modules.validatePermission(st,editPermission());
		}
	}
	
	// ---------- INSTANCE ----------
	public boolean isGenerated() {
		return generated;
	}
	
	@Nonnull
	public String fullName() {
		return module.getName()+"."+meta.name();
	}
	
	@Nonnull
	public KVSCOPE scope() {
		return meta.scope();
	}
	
	@Nonnull
	public KVTYPE type() {
		return meta.type();
	}
	
	@Nonnull
	public String description() {
		return meta.description();
	}
	
	@Nonnull
	public String editPermission() {
		return meta.editPermission();
	}
	
	@Nonnull
	public String defaultValue() {
		if (Config.getGrid()==Config.GRID.OSGRID) {
			if (!meta.defaultValueOSGrid().isEmpty()) {
				return meta.defaultValueOSGrid();
			}
		}
		return meta.defaultValue();
	}
	
	@Nonnull
	public String conveyAs() {
		return meta.conveyAs();
	}
	
	@Nonnull
	@Override
	public String onUpdate() {
		return meta.onUpdate();
	}
	
	@Nonnull
	public KVHIERARCHY hierarchy() {
		return meta.hierarchy();
	}
	
	public boolean hidden() {
		return meta.hidden();
	}
	
	public boolean template() {
		return meta.template();
	}
	
	@Nonnull
	public String name() {
		return meta.name();
	}
	
}
