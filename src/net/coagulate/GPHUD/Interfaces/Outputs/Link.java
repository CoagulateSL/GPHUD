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
public class Link implements Renderable {
	final Renderable content;
	final String target;

	public Link(final String label,
	            final String target) {
		content=new Text(label);
		this.target=target;
	}

	public Link(final Renderable label,
	            final String target) {
		content=label;
		this.target=target;
	}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String asText(final State st) {
		return ">"+content.asText(st)+"<";
	}

	@Nonnull
	@Override
	public String asHtml(final State st,
	                     final boolean rich) {
		return "<a href=\""+target+"\">"+content.asHtml(st,rich)+"</a>";
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		final Set<Renderable> r=new HashSet<>();
		r.add(content);
		return r;
	}
}
