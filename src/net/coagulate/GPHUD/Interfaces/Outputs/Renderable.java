package net.coagulate.GPHUD.Interfaces.Outputs;

import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * For all things that can appear in output.
 * All the formats of output we need.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public interface Renderable {
	/**
	 * Render this element as plain text.
	 * Used for textual output into Second Life (llSay() etc)
	 *
	 * @param st state
	 * @return this element as text
	 */
	@Nonnull
	String asText(State st);

	/**
	 * Render this element into simple HTML.
	 * Used for non admin HTML interfaces like the HUD's web panel and Admin interface.
	 * non admin interface sets rich to false - dont link to admin pages for entities etc.
	 *
	 * @param st state
	 * @param rich Rich mode
	 * @return this element as html
	 */
	@Nonnull
	String asHtml(State st, boolean rich);

	@Nullable
	Set<Renderable> getSubRenderables();
}
