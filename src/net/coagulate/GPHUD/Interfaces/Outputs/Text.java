package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import java.util.Set;

/**
 * Implements plain text, but not like a 'paragraph', more like in a table cell.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Text implements Renderable {
	final String content;

	public Text(String s) { content = s; }

	@Override
	public String asText(State st) {
		return content;
	}

	@Override
	public String asHtml(State st, boolean rich) {
		return content;
	}

	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}
}
