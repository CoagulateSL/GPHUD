package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Raw message thats an OK markup.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class TextOK implements Renderable {
	protected final String content;

    public TextOK(final String content) {
        this.content = content;
    }


	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public String asText(final State st) {
        return "OK : " + content;
	}

	@Nonnull
	@Override
	public String asHtml(final State st,
	                     final boolean rich) {
        return "<font color=green>OK : " + content + "</font>";
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}
}
