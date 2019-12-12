package net.coagulate.GPHUD.Modules.Experience;

import net.coagulate.GPHUD.Modules.KV;

/**
 * @author Iain Price
 */
public class GenericXPLimitKV extends KV {
	private final String myname;

	public GenericXPLimitKV(String name) { myname = name; }

	public boolean isGenerated() { return true; }

	public String fullname() { return "Experience." + myname; }

	public KVSCOPE scope() { return KVSCOPE.NONSPATIAL; }

	public KVTYPE type() { return KVTYPE.INTEGER; }

	public String description() { return "Maximum " + myname + " per cycle"; }

	public String editpermission() { return "Experience.ConfigureXP"; }

	public String defaultvalue() { return "1000"; }

	public String conveyas() { return null; }

	public KVHIERARCHY hierarchy() { return KVHIERARCHY.CUMULATIVE; }

	public boolean template() { return true; }

	public String name() { return myname; }
}
