package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.GPHUD.Modules.KV;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class GenericXPLimitKV extends KV {
	private final String myname;

	public GenericXPLimitKV(final String name) { myname=name; }

	// ---------- INSTANCE ----------
	public boolean isGenerated() { return true; }

	@Nonnull
	public String fullName() { return "Experience."+myname; }

	@Nonnull
	public KVSCOPE scope() { return KVSCOPE.NONSPATIAL; }

	@Nonnull
	public KVTYPE type() { return KVTYPE.INTEGER; }

	@Nonnull
	public String description() { return "Maximum "+myname+" per cycle"; }

	@Nonnull
	public String editPermission() { return "Experience.ConfigureXP"; }

	@Nonnull
	public String defaultValue() { return "1000"; }

	@Nonnull
	public String conveyAs() { return ""; }

	@Nonnull
	public KVHIERARCHY hierarchy() { return KVHIERARCHY.CUMULATIVE; }

	public boolean template() { return true; }

	@Nonnull
	public String name() { return myname; }
}
