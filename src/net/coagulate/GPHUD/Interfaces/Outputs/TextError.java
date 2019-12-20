package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Raw message thats an error markup.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class TextError implements Renderable {
	protected final String s;

	public TextError(final String s) { this.s=s; }

	public String getMessage(final State st) { return s; }

	@Nonnull
	@Override
	public String asText(final State st) {
		return ">>> ERROR : "+s;
	}

	@Nonnull
	@Override
	public String asHtml(final State st,
	                     final boolean rich) {
		return "<font color=red><b> *** ERROR : "+s+" ***</b></font>";
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}
}
