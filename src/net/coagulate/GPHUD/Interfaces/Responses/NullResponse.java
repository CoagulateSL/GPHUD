package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * A response that indicates nothing happened.
 * All the methods throw NullPointerException.  This is a special case
 * response used in the HTML interface to signal it laid out a form but
 * didn't process it, because submit was not pressed (HTML forms are a 2
 * stage process - render on first request, process on subequent).
 * <p>
 * Not noticing this is a special case and calling its methods gets you a well
 * deserved exception.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class NullResponse implements Response {

	public NullResponse() {}

	@Nonnull
	@Override
	public JSONObject asJSON(State st) {
		throw new SystemException("You can not interrogate the null response.");
	}

	@Nonnull
	@Override
	public String scriptResponse() {
		throw new SystemException("You can not interrogate the null response.");
	}

	@Nonnull
	@Override
	public String asText(State st) {
		throw new SystemException("You can not interrogate the null response.");
	}

	@Nonnull
	@Override
	public String asHtml(State st, boolean rich) {
		throw new SystemException("You can not interrogate the null response.");
	}

	@Nonnull
	@Override
	public Set<Renderable> getSubRenderables() {
		throw new SystemException("You can not interrogate the null response.");
	}
}
