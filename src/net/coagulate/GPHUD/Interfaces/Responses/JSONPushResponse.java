package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import java.util.Set;

/**
 * A raw response, unprocessed.
 * Returns the JSON to the caller if its an SL script, otherwise pushes the json to the supplied URL and returns an "OK" repsonse.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class JSONPushResponse implements Response {
	JSONObject json;
	String url;
	Response nonjson;

	public JSONPushResponse(JSONObject j, String url, Response nonjson) {
		if (url == null || url.isEmpty() || "?".equals(url)) { throw new SystemException("Can not use the null URL"); }
		json = j;
		this.url = url;
		this.nonjson = nonjson;
	}

	@Override
	public JSONObject asJSON(State st) {
		return json;
	}

	@Override
	public String asText(State st) {
		Transmission t = new Transmission((Char) null, json, url);
		t.start();
		return nonjson.asText(st);
	}

	@Override
	public String asHtml(State st, boolean rich) {
		Transmission t = new Transmission((Char) null, json, url);
		t.start();
		return nonjson.asHtml(st, rich);
	}

	@Override
	public Set<Renderable> getSubRenderables() {
		throw new SystemException("JSONResponse can not be interrogated as a Form");
	}
}
