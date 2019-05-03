package net.coagulate.GPHUD.Interfaces.Inputs;

import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;

import java.util.Set;

/**
 * Implements a single line text input box.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class CheckBox extends Input {
	String name = "";

	public CheckBox(String name) {this.name = name;}

	public CheckBox(String name, String value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String asHtml(State st, boolean rich) {
		String r = "<input type=\"checkbox\" name=\"" + name + "\" ";
		if (value != null && !value.isEmpty()) { r += "checked"; }
		r += " />";
		return r;
	}

	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}
}
