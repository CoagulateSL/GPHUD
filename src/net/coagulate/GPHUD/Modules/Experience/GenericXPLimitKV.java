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
	public String fullname() { return "Experience."+myname; }

	@Nonnull
	public KVSCOPE scope() { return KVSCOPE.NONSPATIAL; }

	@Nonnull
	public KVTYPE type() { return KVTYPE.INTEGER; }

	@Nonnull
	public String description() { return "Maximum "+myname+" per cycle"; }

	@Nonnull
	public String editpermission() { return "Experience.ConfigureXP"; }

	@Nonnull
	public String defaultvalue() { return "1000"; }

	@Nonnull
	public String conveyas() { return ""; }

	@Nonnull
	public KVHIERARCHY hierarchy() { return KVHIERARCHY.CUMULATIVE; }

	public boolean template() { return true; }

	@Nonnull
	public String name() { return myname; }
}
