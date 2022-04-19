package net.coagulate.GPHUD.Modules.Inventory;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Modules.KV;

import javax.annotation.Nonnull;

/**
 * Wraps a dynamic attribute KV for characters.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class InventoryKV extends KV {

	@Nonnull final Attribute inventory;

	protected InventoryKV(@Nonnull final Attribute inventory) {
		this.inventory = inventory;
	}

	// ---------- INSTANCE ----------
	@Override
	public final boolean isGenerated() {
		return true;
	}

	@Nonnull
	@Override
	public final String fullName() {
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
	public String defaultValue() {
		return "0";
	}

	@Nonnull
	@Override
	public final String conveyAs() {
		return "";
	}

	@Nonnull
	@Override
	public KVHIERARCHY hierarchy() {
		return KVHIERARCHY.CUMULATIVE;
	}

	@Override
	public boolean template() { return false; }

	@Nonnull
	@Override
	public final String name() {
		return inventory.getName()+suffix();
	}

	@Nonnull
	protected abstract String suffix();

}
