package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Modules.KV;

import javax.annotation.Nonnull;

/**
 * Wraps a dynamic attribute KV for characters.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class AttributeKV extends KV {

	final Attribute attribute;

	public AttributeKV(final Attribute attribute) {
		this.attribute=attribute;
	}

	// ---------- INSTANCE ----------
	@Override
	public boolean isGenerated() {
		return true;
	}

	@Nonnull
	@Override
	public String fullName() {
		return "Characters."+name();
	}

	@Nonnull
	@Override
	public KVSCOPE scope() {
		return KVSCOPE.COMPLETE;
	}

	@Nonnull
	@Override
	public KVTYPE type() {
		return attribute.getKVType();
	}

	@Nonnull
	@Override
	public String description() {
		return "Character attribute "+attribute.getName();
	}

	@Nonnull
	@Override
	public String editPermission() {
		return "Characters.Set"+name();
	}

	@Nonnull
	@Override
	public String defaultValue() {
		return attribute.getKVDefaultValue();
	}

	@Nonnull
	@Override
	public String conveyAs() {
		return "";
	}

	@Nonnull
	@Override
	public KVHIERARCHY hierarchy() {
		return attribute.getKVHierarchy();
	}

	@Override
	public boolean template() { return attribute.templatable(); }

	@Nonnull
	@Override
	public String name() {
		return attribute.getName();
	}

}
