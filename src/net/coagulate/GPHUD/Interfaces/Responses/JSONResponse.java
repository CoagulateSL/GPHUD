package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * A raw response, unprocessed.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class JSONResponse implements Response {
	final JSONObject json;

	public JSONResponse(final JSONObject j) {
		json=j;
	}

	// ---------- INSTANCE ----------
	@Override
	public JSONObject asJSON(final State st) {
		return json;
	}

	@Nonnull
	@Override
	public String scriptResponse() {
		return "<A JSON Response>";
	}

	@Nonnull
	@Override
	public String asText(final State st) {
		throw new SystemImplementationException("JSONResponse can not be converted to Text");
	}

	@Nonnull
	@Override
	public String asHtml(final State st,
	                     final boolean rich) {
		throw new SystemImplementationException("JSONResponse can not be converted to HTML");
	}

	@Nullable
	@Override
	public Set<Renderable> getSubRenderables() {
		throw new SystemImplementationException("JSONResponse can not be interrogated as a Form");
	}

	/** Target will emit message under guise of the sayAs name */
	public static void sayAs(@Nonnull JSONObject json,
							 @Nullable String sayAs,
							 @Nonnull String message) {
		int i=1;
		while (json.has("say"+i)) { i++; }
		if (sayAs!=null) { json.put("sayas",sayAs); }
		json.put("say"+i,message);
	}
	/** Target will emit message under guise of the sayAs name */
	public void sayAs(@Nullable String sayAs,@Nonnull String message) { sayAs(json,sayAs,message); }
	/** Target will emit message .. not really sure why this differs from sayashud at this point*/
	public static void say(@Nonnull JSONObject json,@Nonnull String message) { sayAs(json,null,message); }
	/** Target will emit message .. not really sure why this differs from sayashud at this point*/
	public void say(@Nonnull String message) { sayAs(null,message); }
	/** Target will emit message as the GPHUD */
	public static void sayAsHud(@Nonnull JSONObject json,
								@Nonnull String message) {
		int i=1;
		while (json.has("sayashud"+i)) { i++; }
		json.put("sayashud"+i,message);
	}
	/** Target will emit message as the GPHUD */
	public void sayAsHud(@Nonnull String message) { sayAsHud(json,message); }
	/** Target will message the owner (does not work on objects) */
	public static void ownerSay(@Nonnull JSONObject json,
								@Nonnull String message) {
		int i=1;
		while (json.has("message"+i)) { i++; }
		json.put("message"+i,message);
	}
	/** Target will message the owner (does not work on objects) */
	public void ownerSay(@Nonnull String message) { ownerSay(json,message); }
	/** Target will message the owner (does not work on objects) (alias for ownerSay)*/
	public static void message(@Nonnull JSONObject json,
							   @Nonnull String message) { ownerSay(json,message); }
	/** Target will message the owner (does not work on objects) (alias for ownerSay*/
	public void message(@Nonnull String message) { message(json,message); }
}
