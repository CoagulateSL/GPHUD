package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Just a hr tag.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class Separator implements Renderable {

	@Nonnull
	@Override
	public String asText(State st) {
		return "----------\n";
	}

	@Nonnull
	@Override
	public String asHtml(State st, boolean rich) {
		return "<hr>";
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}

}
