package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.GPHUD.Data.Attribute;

import javax.annotation.Nonnull;

/**
 * Wraps a dynamic attribute KV for characters.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class AccessibleKV extends InventoryKV {
	public AccessibleKV(@Nonnull final Attribute inventory) {
		super(inventory);
	}

	@Nonnull
	@Override
	public KVTYPE type() {
		return KVTYPE.BOOLEAN;
	}

	@Nonnull
	@Override
	public String description() {
		return "Wether an inventory "+inventory.getName()+" is accessible";
	}

	@Nonnull
	@Override
	public String editPermission() {
		return "Inventory.ConfigureAccess";
	}

	@Nonnull
	@Override
	public String defaultValue() {
		return "true";
	}

	@Nonnull
	@Override
	public KVHIERARCHY hierarchy() {
		return KVHIERARCHY.DELEGATING;
	}

	@Nonnull
	@Override
	protected String suffix() {
		return "Accessible";
	}

}
