package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Paragraph.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Paragraph implements Renderable {

	final Renderable content;

	public Paragraph(final String s) { content=new Text(s); }

	public Paragraph(final Renderable r) { content=r; }

	public Paragraph() {
		content=new Text("");
	}

	@Nonnull
	@Override
	public String asText(final State st) {
		return content.asText(st)+"\n";
	}

	@Nonnull
	@Override
	public String asHtml(final State st,
	                     final boolean rich)
	{
		return "<p>"+content.asHtml(st,rich)+"</p>";
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		final Set<Renderable> r=new HashSet<>();
		r.add(content);
		return r;
	}
}
