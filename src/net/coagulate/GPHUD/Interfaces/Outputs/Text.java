package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Implements plain text, but not like a 'paragraph', more like in a table cell.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Text implements Renderable {
	final String content;
	
	public Text(@Nonnull final String s) {
		content=s;
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String asText(final State st) {
		return content;
	}
	
	@Nonnull
	@Override
	public String asHtml(final State st,final boolean rich) {
		return content;
	}
	
	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}
}
