package net.coagulate.GPHUD.Interfaces.Inputs;

import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;

import java.util.Set;

/**
 * Hidden input element.
 *
 * @author iain
 */
public class Hidden extends Input {

	final String name;

	public Hidden(String name, String value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String asHtml(State st, boolean rich) {
		return "<input type=hidden name=\"" + getName() + "\" value=\"" + value + "\">";
	}

	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}

}
