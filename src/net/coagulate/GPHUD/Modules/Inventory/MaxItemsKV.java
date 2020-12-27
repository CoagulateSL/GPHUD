package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.GPHUD.Data.Inventory;
import net.coagulate.GPHUD.Modules.KV;

import javax.annotation.Nonnull;

/**
 * Wraps a dynamic attribute KV for characters.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class MaxItemsKV extends KV {

	@Nonnull final Inventory inventory;

	public MaxItemsKV(@Nonnull final Inventory inventory) {
		this.inventory=inventory;
	}

	// ---------- INSTANCE ----------
	@Override
	public boolean isGenerated() {
		return true;
	}

	@Nonnull
	@Override
	public String fullname() {
		return "Inventory."+name();
	}

	@Nonnull
	@Override
	public KVSCOPE scope() {
		return KVSCOPE.COMPLETE;
	}

	@Nonnull
	@Override
	public KVTYPE type() {
		return KVTYPE.INTEGER;
	}

	@Nonnull
	@Override
	public String description() {
		return "Maximum number of types of items storable in "+inventory.getName();
	}

	@Nonnull
	@Override
	public String editpermission() {
		return "Inventory.ConfigureLimits";
	}

	@Nonnull
	@Override
	public String defaultvalue() {
		return "0";
	}

	@Nonnull
	@Override
	public String conveyas() {
		return "";
	}

	@Nonnull
	@Override
	public KVHIERARCHY hierarchy() {
		return KVHIERARCHY.CUMULATIVE;
	}

	@Override
	public boolean template() { return true; }

	@Nonnull
	@Override
	public String name() {
		return inventory.getName()+"MaxItems";
	}

}
