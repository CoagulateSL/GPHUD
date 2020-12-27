package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.GPHUD.Data.Attribute;

import javax.annotation.Nonnull;

/**
 * Wraps a dynamic attribute KV for characters.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class DefaultAllowKV extends InventoryKV {

	public DefaultAllowKV(@Nonnull Attribute inventory) {
		super(inventory);
	}

	@Nonnull
	@Override
	public KVSCOPE scope() {
		return KVSCOPE.INSTANCE;
	}

	@Nonnull
	@Override
	public KVTYPE type() {
		return KVTYPE.BOOLEAN;
	}

	@Nonnull
	@Override
	public String description() {
		return "Wether an inventory "+inventory.getName()+" is allowed to store new items by default";
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
		return "DefaultAllow";
	}

}
