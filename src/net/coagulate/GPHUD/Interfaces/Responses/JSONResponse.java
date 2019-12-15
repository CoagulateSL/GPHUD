package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * A raw response, unprocessed.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class JSONResponse implements Response {
	final JSONObject json;

	public JSONResponse(final JSONObject j) {
		json = j;
	}

	@Override
	public JSONObject asJSON(final State st) {
		return json;
	}

	@Nonnull
	@Override
	public String scriptResponse() {
		return "<A JSON Response>";
	}

	@Nonnull
	@Override
	public String asText(final State st) {
		throw new SystemImplementationException("JSONResponse can not be converted to Text");
	}

	@Nonnull
	@Override
	public String asHtml(final State st, final boolean rich) {
		throw new SystemImplementationException("JSONResponse can not be converted to HTML");
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		throw new SystemImplementationException("JSONResponse can not be interrogated as a Form");
	}
}
