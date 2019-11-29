package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Modules.KV;

/**
 * Wraps a dynamic attribute KV for characters.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ConveyanceKV extends KV {

	final String name;

	public ConveyanceKV(String attribute) {
		this.name = attribute;
	}

	@Override
	public boolean isGenerated() {
		return true;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public KVSCOPE scope() {
		return KVSCOPE.CHARACTER;
	}

	@Override
	public KVTYPE type() {
		return KVTYPE.TEXT;
	}

	@Override
	public String description() {
		return "Conveyance memory, internal use only";
	}

	@Override
	public String editpermission() {
		return "instance.owner";
	}

	@Override
	public String defaultvalue() {
		return "";
	}

	@Override
	public String conveyas() {
		return ""; // NEVER.  NO.  DONT.  :P
	}

	@Override
	public KVHIERARCHY hierarchy() {
		return KVHIERARCHY.NONE;
	}

	@Override
	public String fullname() {
		return "GPHUDClient." + name();
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
