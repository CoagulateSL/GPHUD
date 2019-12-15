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
public class TextInput extends Input {
	String name = "";
	@Nullable
	Integer width;

	public TextInput(final String name) {this.name = name;}

	public TextInput(final String name, final String value) {
		this.name = name;
		this.value = value;
	}

	public TextInput(final String name, final int width) {
		this.name = name;
		this.width = width;
	}

	public TextInput(final String name, final String value, final int width) {
		this.name = name;
		this.value = value;
		this.width = width;
	}

	@Nonnull
	String getType() { return "text"; }

	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	@Override
	public String asHtml(final State st, final boolean rich) {
		String s = "<input type=\"" + getType() + "\" name=\"" + name + "\" value=\"" + value + "\" ";
		if (width != null) { s += "size=" + width + " "; }
		//if (!(st.handler instanceof net.coagulate.GPHUD.Interfaces.HUD.Interface)) {
		s += "autofocus ";
		//}
		s += "/>";
		return s;
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}
}
