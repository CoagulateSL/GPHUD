package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import java.util.Set;

/**
 * A raw response, unprocessed.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class JSONResponse implements Response {
	JSONObject json;

	public JSONResponse(JSONObject j) {
		json = j;
	}

	@Override
	public JSONObject asJSON(State st) {
		return json;
	}

	@Override
	public String scriptResponse() {
		return "<A JSON Response>";
	}

	@Override
	public String asText(State st) {
		throw new SystemException("JSONResponse can not be converted to Text");
	}

	@Override
	public String asHtml(State st, boolean rich) {
		throw new SystemException("JSONResponse can not be converted to HTML");
	}

	@Override
	public Set<Renderable> getSubRenderables() {
		throw new SystemException("JSONResponse can not be interrogated as a Form");
	}
}
