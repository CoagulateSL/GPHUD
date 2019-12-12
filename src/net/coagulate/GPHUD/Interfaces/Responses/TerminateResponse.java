package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import java.util.Set;

/**
 * A response that terminates the remote endpoint
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class TerminateResponse implements Response {

	final String reason;

	public TerminateResponse(String r) {
		reason = r;
	}

	@Override
	public JSONObject asJSON(State st) {
		JSONObject j = new JSONObject();
		j.put("terminate", reason);
		return j;
	}

	@Override
	public String scriptResponse() {
		return "<The TERMINATE-NOW Response> (how did you get this!?";
	}

	@Override
	public String asText(State st) {
		throw new SystemException("This request is TERMINATED - " + reason);
	}

	@Override
	public String asHtml(State st, boolean rich) {
		throw new SystemException("This request is TERMINATED - " + reason);
	}

	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}
}
