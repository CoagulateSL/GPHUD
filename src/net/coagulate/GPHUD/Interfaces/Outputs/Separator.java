package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import java.util.Set;

/**
 * Just a hr tag.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Separator implements Renderable {

	@Override
	public String asText(State st) {
		return "----------\n";
	}

	@Override
	public String asHtml(State st, boolean rich) {
		return "<hr>";
	}

	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}

}
