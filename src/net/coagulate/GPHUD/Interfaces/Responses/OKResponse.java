package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.GPHUD.Interfaces.Outputs.TextOK;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

/**
 * Default implementation of a Response, the OK Response.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class OKResponse extends TextOK implements Response {
	public OKResponse(String message) { super(message); }

	@Override
	public JSONObject asJSON(State st) {
		JSONObject j = new JSONObject();
		j.put("message", asText(st));
		return j;
	}

	@Override
	public String scriptResponse() {
		return "OK: "+s;
	}

}
