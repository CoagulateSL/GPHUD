package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import java.util.Set;

/**
 * Raw message thats an OK markup.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class TextOK implements Renderable {
	protected String s;

	public TextOK(String s) { this.s = s; }


	@Override
	public String asText(State st) {
		return "OK : " + s;
	}

	@Override
	public String asHtml(State st, boolean rich) {
		return "<font color=green>OK : " + s + "</font>";
	}

	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}
}
