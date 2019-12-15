package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * A response that terminates the remote endpoint
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class TerminateResponse implements Response {

	final String reason;

	public TerminateResponse(final String r) {
		reason = r;
	}

	@Nonnull
	@Override
	public JSONObject asJSON(final State st) {
		final JSONObject j = new JSONObject();
		j.put("terminate", reason);
		return j;
	}

	@Nonnull
	@Override
	public String scriptResponse() {
		return "<The TERMINATE-NOW Response> (how did you get this!?";
	}

	@Nonnull
	@Override
	public String asText(final State st) {
		throw new SystemConsistencyException("This request is TERMINATED - " + reason);
	}

	@Nonnull
	@Override
	public String asHtml(final State st, final boolean rich) {
		throw new SystemConsistencyException("This request is TERMINATED - " + reason);
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		return null;
	}
}
