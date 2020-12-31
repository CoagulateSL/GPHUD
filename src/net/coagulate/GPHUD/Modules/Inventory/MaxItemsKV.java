package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.GPHUD.Data.Attribute;

import javax.annotation.Nonnull;

/**
 * Wraps a dynamic attribute KV for characters.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class MaxItemsKV extends InventoryKV {

	public MaxItemsKV(@Nonnull Attribute inventory) {
		super(inventory);
	}

	@Nonnull
	@Override
	public String description() {
		return "Maximum number of types of items storable in "+inventory.getName();
	}

	@Nonnull
	@Override
	public String editPermission() {
		return "Inventory.ConfigureLimits";
	}

	@Override
	public boolean template() { return true; }

	@Nonnull
	@Override
	protected String suffix() {
		return "MaxItems";
	}
}