package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.GPHUD.Interfaces.Outputs.TextOK;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * Default implementation of a Response, the OK Response.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class OKResponse extends TextOK implements Response {
	public OKResponse(final String message) { super(message); }

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public JSONObject asJSON(final State st) {
		final JSONObject j=new JSONObject();
		j.put("message",asText(st));
		return j;
	}

	@Nonnull
	@Override
	public String scriptResponse() {
		return "OK: "+s;
	}

}
