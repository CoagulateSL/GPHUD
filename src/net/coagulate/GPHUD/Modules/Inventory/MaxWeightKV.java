package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Modules.KV;

import javax.annotation.Nonnull;

/**
 * Wraps a dynamic attribute KV for characters.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class MaxWeightKV extends KV {

	@Nonnull final Attribute inventory;

	public MaxWeightKV(@Nonnull final Attribute inventory) {
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
		return "Maximum total quantity of weight storable in "+inventory.getName();
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
		return inventory.getName()+"MaxWeight";
	}

}
