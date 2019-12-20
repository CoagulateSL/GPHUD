package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * A raw response, unprocessed.
 * Returns the JSON to the caller if its an SL script, otherwise pushes the json to the supplied URL and returns an "OK" repsonse.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class JSONPushResponse implements Response {
	JSONObject json;
	@Nullable
	String url;
	Response nonjson;

	public JSONPushResponse(final JSONObject j,
	                        @Nullable final String url,
	                        final Response nonjson) {
		if (url==null || url.isEmpty() || "?".equals(url)) {
			throw new SystemBadValueException("Can not use the null URL");
		}
		json=j;
		this.url=url;
		this.nonjson=nonjson;
	}

	@Override
	public JSONObject asJSON(final State st) {
		return json;
	}

	@Nonnull
	@Override
	public String scriptResponse() {
		return "<A JSON Push Response>";
	}

	@Nonnull
	@Override
	public String asText(final State st) {
		final Transmission t=new Transmission((Char) null,json,url);
		t.start();
		return nonjson.asText(st);
	}

	@Nonnull
	@Override
	public String asHtml(final State st,
	                     final boolean rich) {
		final Transmission t=new Transmission((Char) null,json,url);
		t.start();
		return nonjson.asHtml(st,rich);
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		throw new SystemImplementationException("JSONResponse can not be interrogated as a Form");
	}
}
