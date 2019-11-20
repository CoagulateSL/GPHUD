package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import java.util.Set;

/**
 * Raw message thats an error markup.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class TextError implements Renderable {
	protected String s;

	public TextError(String s) { this.s = s; }

	public String getMessage(State st) { return s; }

	@Override
	public String asText(State st) {
		return ">>> ERROR : " + s;
	}

	@Override
	public String asHtml(State st, boolean rich) {
		return "<font color=red><b> *** ERROR : " + s + " ***</b></font>";
	}

	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}
}
