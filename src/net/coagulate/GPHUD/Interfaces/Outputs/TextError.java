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

	public TextError(String s) { this.s = s; }

	public String getMessage(State st) { return s; }

	@Nonnull
	@Override
	public String asText(State st) {
		return ">>> ERROR : " + s;
	}

	@Nonnull
	@Override
	public String asHtml(State st, boolean rich) {
		return "<font color=red><b> *** ERROR : " + s + " ***</b></font>";
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}
}
