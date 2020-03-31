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
public class NoResponse extends TextOK implements Response {
	public NoResponse() { super(""); }

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public JSONObject asJSON(final State st) {
		return new JSONObject();
	}

	@Nonnull
	@Override
	public String scriptResponse() {
		return "<No Response>";
	}

}
