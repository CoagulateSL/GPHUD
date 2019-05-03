package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.GPHUD.Interfaces.Outputs.TextOK;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

/**
 * Default implementation of a Response, the OK Response.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class NoResponse extends TextOK implements Response {
	public NoResponse() { super(""); }

	@Override
	public JSONObject asJSON(State st) {
		JSONObject j = new JSONObject();
		return j;
	}

}
