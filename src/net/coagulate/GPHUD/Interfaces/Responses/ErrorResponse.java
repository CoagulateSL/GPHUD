package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.GPHUD.Interfaces.Outputs.TextError;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

/**
 * A response that represents an error with the command.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ErrorResponse extends TextError implements Response {

	public ErrorResponse(String r) {
		super(r);
	}

	@Override
	public JSONObject asJSON(State st) {
		JSONObject j = new JSONObject();
		j.put("error", asText(st));
		return j;
	}

	@Override
	public String scriptResponse() {
		return "Error: "+s;
	}

}
