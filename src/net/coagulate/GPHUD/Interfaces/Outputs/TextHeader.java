package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Implements a header element.
 * Hmm, this is bad, the content should just be an element, not forced text.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class TextHeader implements Renderable {
	final Renderable content;

	public TextHeader(final String s) { content = new Text(s); }

	public TextHeader(final Renderable r) { content = r; }

	@Nonnull
	@Override
	public String asText(final State st) {
		return "===== " + content.asText(st) + " =====\n";
	}

	@Nonnull
	@Override
	public String asHtml(final State st, final boolean rich) {
		return "<h1>" + content.asHtml(st, rich) + "</h1>";
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		final Set<Renderable> r = new HashSet<>();
		r.add(content);
		return r;
	}
}
