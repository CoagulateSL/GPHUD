package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import java.util.Set;

/**
 * Default implementation of a Response, the (less than) OK Response.
 * Almost identical to the OK response but without the OK prefix or web component part.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class SimpleResponse implements Response {
	final String message;

	public SimpleResponse(String message) { this.message = message; }

	@Override
	public JSONObject asJSON(State st) {
		return new JSONObject().put("message", message);
	}

	@Override
	public String scriptResponse() {
		return message;
	}

	@Override
	public String asText(State st) {
		return message;
	}

	@Override
	public String asHtml(State st, boolean rich) {
		return message;
	}

	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}


}
