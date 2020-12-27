package net.coagulate.GPHUD.Modules.TemplateWrapper;

import net.coagulate.GPHUD.Modules.KV;

import javax.annotation.Nonnull;

/**
 * Wraps a dynamic attribute KV for characters.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class TemplatePostfix extends KV {

	final String template;

	public TemplatePostfix(final String template) {
		this.template=template;
	}

	// ---------- INSTANCE ----------
	@Override
	public boolean isGenerated() {
		return true;
	}

	@Nonnull
	@Override
	public String fullName() {
		return "TemplateWrapper."+name();
	}

	@Nonnull
	@Override
	public KVSCOPE scope() {
		return KVSCOPE.COMPLETE;
	}

	@Nonnull
	@Override
	public KVTYPE type() {
		return KVTYPE.TEXT;
	}

	@Nonnull
	@Override
	public String description() {
		return "Postfix for wrapped form of template "+template;
	}

	@Nonnull
	@Override
	public String editPermission() {
		return "TemplateWrapper.EditAffix";
	}

	@Nonnull
	@Override
	public String defaultValue() {
		return ">";
	}

	@Nonnull
	@Override
	public String conveyAs() {
		return "";
	}

	@Nonnull
	@Override
	public KVHIERARCHY hierarchy() {
		return KVHIERARCHY.DELEGATING;
	}

	@Override
	public boolean template() {
		return false; // template what we wrap, not us
	}

	@Nonnull
	@Override
	public String name() {
		return template+"Postfix";
	}

}
