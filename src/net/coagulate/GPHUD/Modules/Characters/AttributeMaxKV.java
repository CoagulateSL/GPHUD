package net.coagulate.GPHUD.Modules.Characters;

import net.coagulate.GPHUD.Data.Attribute;
import net.coagulate.GPHUD.Modules.KV;

import javax.annotation.Nonnull;

/**
 * Wraps a dynamic attribute KV for characters.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class AttributeMaxKV extends KV {

	final Attribute attribute;

	public AttributeMaxKV(final Attribute attribute) {
		this.attribute = attribute;
	}

	@Override
	public boolean isGenerated() {
		return true;
	}

	@Nonnull
	@Override
	public String name() {
		return attribute.getName() + "MAX";
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
		return "Character attribute " + attribute.getName() + " maximum allowed value";
	}

	@Nonnull
	@Override
	public String editpermission() {
		return "Characters.CreateAttribute";
	}

	@Nonnull
	@Override
	public String defaultvalue() {
		return attribute.getKVDefaultValue();
	}

	@Nonnull
	@Override
	public String conveyas() {
		return "";
	}

	@Nonnull
	@Override
	public KVHIERARCHY hierarchy() {
		return attribute.getKVHierarchy();
	}

	@Nonnull
	@Override
	public String fullname() {
		return "Characters." + name();
	}

	@Override
	public boolean template() {
		return true;
	}

}
