package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

/**
 * Implements a header element.
 * Hmm, this is bad, the content should just be an element, not forced text.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class TextSubHeader implements Renderable {

	final Renderable content;

	public TextSubHeader(String s) { content = new Text(s); }

	public TextSubHeader(Renderable r) { content = r; }

	@Nonnull
	@Override
	public String asText(State st) {
		return "=== " + content.asText(st) + " ===\n";
	}

	@Nonnull
	@Override
	public String asHtml(State st, boolean rich) {
		return "<h3>" + content.asHtml(st, rich) + "</h3>";
	}

	@Nonnull
	@Override
	public Set<Renderable> getSubRenderables() {
		Set<Renderable> r = new HashSet<>();
		r.add(content);
		return r;
	}
}
