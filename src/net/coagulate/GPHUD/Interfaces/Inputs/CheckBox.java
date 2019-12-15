package net.coagulate.GPHUD.Interfaces.Inputs;

import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Implements a single line text input box.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class CheckBox extends Input {
	String name = "";

	public CheckBox(final String name) {this.name = name;}

	public CheckBox(final String name, final String value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	@Override
	public String asHtml(final State st, final boolean rich) {
		String r = "<input type=\"checkbox\" name=\"" + name + "\" ";
		if (value != null && !value.isEmpty()) { r += "checked"; }
		r += " />";
		return r;
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}
}
