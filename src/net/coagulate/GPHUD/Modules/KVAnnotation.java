package net.coagulate.GPHUD.Modules;

import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

/**
 * Wraps a KV (Key Value) element.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class KVAnnotation extends KV {
	KVS meta;
	boolean generated=true;
	Module module;

	KVAnnotation() {}

	public KVAnnotation(final Module m,
	                    final KVS meta) {
		module=m;
		this.meta=meta;
		validate(null);
		generated=false;
	}

	// ---------- INSTANCE ----------
	public boolean isGenerated() { return generated; }

	@Nonnull
	public String fullname() { return module.getName()+"."+meta.name(); }

	@Nonnull
	public KVSCOPE scope() { return meta.scope(); }

	@Nonnull
	public KVTYPE type() { return meta.type(); }

	@Nonnull
	public String description() { return meta.description(); }

	@Nonnull
	public String editpermission() { return meta.editpermission(); }

	@Nonnull
	public String defaultvalue() { return meta.defaultvalue(); }

	@Nonnull
	public String conveyas() { return meta.conveyas(); }

	@Nonnull
	public KVHIERARCHY hierarchy() { return meta.hierarchy(); }

	public boolean template() { return meta.template(); }

	public boolean hidden() { return meta.hidden(); }

	@Nonnull
	public String name() { return meta.name(); }

	// ----- Internal Instance -----
	private void validate(final State st) {
		if (!editpermission().isEmpty()) {
			Modules.validatePermission(st,editpermission());
		}
	}

}
