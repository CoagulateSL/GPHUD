package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Modules.KV;

import javax.annotation.Nonnull;

/**
 * Wraps a dynamic attribute KV for characters.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ConveyanceKV extends KV {

	final String name;

	public ConveyanceKV(final String attribute) {
		name=attribute;
	}

	// ---------- INSTANCE ----------
	@Override
	public boolean isGenerated() {
		return true;
	}

	@Nonnull
	@Override
	public String fullname() {
		return "GPHUDClient."+name();
	}

	@Nonnull
	@Override
	public KVSCOPE scope() {
		return KVSCOPE.CHARACTER;
	}

	@Nonnull
	@Override
	public KVTYPE type() {
		return KVTYPE.TEXT;
	}

	@Nonnull
	@Override
	public String description() {
		return "Conveyance memory, internal use only";
	}

	@Nonnull
	@Override
	public String editpermission() {
		return "instance.owner";
	}

	@Nonnull
	@Override
	public String defaultvalue() {
		return "";
	}

	@Nonnull
	@Override
	public String conveyas() {
		return ""; // NEVER.  NO.  DONT.  :P
	}

	@Nonnull
	@Override
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

	@Nonnull
	@Override
	public String name() {
		return name;
	}

}
