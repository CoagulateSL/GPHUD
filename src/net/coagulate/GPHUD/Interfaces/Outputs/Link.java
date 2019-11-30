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
public class Link implements Renderable {
	final Renderable content;
	final String target;

	public Link(String label, String target) {
		content = new Text(label);
		this.target = target;
	}

	public Link(Renderable label, String target) {
		content = label;
		this.target = target;
	}

	@Nonnull
	@Override
	public String asText(State st) {
		return ">" + content.asText(st) + "<";
	}

	@Nonnull
	@Override
	public String asHtml(State st, boolean rich) {
		return "<a href=\"" + target + "\">" + content.asHtml(st, rich) + "</a>";
	}

	@Nonnull
	@Override
	public Set<Renderable> getSubRenderables() {
		Set<Renderable> r = new HashSet<>();
		r.add(content);
		return r;
	}
}
