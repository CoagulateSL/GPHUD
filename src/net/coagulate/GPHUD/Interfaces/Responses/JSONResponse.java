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


	/* The 'HUD say' mess
	There have been 3 versions of the HUD messaging (say/sayas/ownersay) protocol:
	** Version 1 supported a singular say/sayas/message, which worked great until scripts came along and people made long
	text messages.  Version 1 solved this by adding them onto existing messages, however this suffers the problems
	version 2 suffers (see below), as well as having a line length limitation
	Version 1 was officially terminated through End Of Lifing, though that said Version 2 supports version 1 format too.
	** Version 2 supported say1 say2 say3 etc and the same for other message types.  This suffers from the limitation
	that all messaages of a type are emitted in order, so if you code a "say, sayas, say", then the two says happen
	first (or last) rather than around the other type of chat.
	** Version 3 which is supported by connection specific protocol identifiers uses an "output1"/"output2" etc format
	the first character of the value indicates the output type (s for say, a for sayas, o for ownersay/message)
	for say as types, the name to say "as" is then added to the value, followed by a | and then the message to output
	For non say as types, the string following the first character is output.
	 */

	public static String findFreePrefix(@Nonnull JSONObject json,
										@Nonnull String prefix) {
		int i=1;
		while (json.has(prefix+i)) { i++; }
		return prefix+i;
	}

	/** Target will emit message under guise of the sayAs name */
	public static void sayAs(@Nonnull JSONObject json,
							 @Nullable String sayAs,
							 @Nonnull String message,
							 final int protocol) {
		if (protocol<3) {
			if (sayAs != null) {
				json.put("sayas", sayAs);
			}
			json.put(findFreePrefix(json, "say"), message);
		} else {
			if (sayAs==null) {
				json.put(findFreePrefix(json, "output"), "s" + message);
			} else {
				json.put(findFreePrefix(json, "output"), "a" + sayAs+"|"+message);
			}
		}
	}
	/** Target will emit message under guise of the sayAs name */
	public void sayAs(@Nullable String sayAs,@Nonnull String message,final int protocol) { sayAs(json,sayAs,message,protocol); }
	/** Target will emit message .. not really sure why this differs from sayashud at this point*/
	public static void say(@Nonnull JSONObject json,@Nonnull String message,final int protocol) { sayAs(json,null,message,protocol); }
	/** Target will emit message .. not really sure why this differs from sayashud at this point*/
	public void say(@Nonnull String message,final int protocol) { sayAs(null,message,protocol); }
	/** Target will emit message as the GPHUD */
	public static void sayAsHud(@Nonnull JSONObject json,
								@Nonnull String message,
								final int protocol) {
		if (protocol<3) {
			json.put(findFreePrefix(json,"sayashud"),message);
		} else {
			json.put(findFreePrefix(json, "output"), "s" + message);
		}
	}
	/** Target will emit message as the GPHUD */
	public void sayAsHud(@Nonnull String message,final int protocol) { sayAsHud(json,message,protocol); }
	/** Target will message the owner (does not work on objects) */
	public static void ownerSay(@Nonnull JSONObject json,
								@Nonnull String message,
								final int protocol) {
		if (protocol<3) {
			json.put(findFreePrefix(json,"message"),message);
		} else {
			json.put(findFreePrefix(json, "output"), "o" + message);
		}
	}
	/** Target will message the owner (does not work on objects) */
	public void ownerSay(@Nonnull String message,final int protocol) { ownerSay(json,message,protocol); }
	/** Target will message the owner (does not work on objects) (alias for ownerSay)*/
	public static void message(@Nonnull JSONObject json,
							   @Nonnull String message,
							   final int protocol) { ownerSay(json,message,protocol); }
	/** Target will message the owner (does not work on objects) (alias for ownerSay*/
	public void message(@Nonnull String message,final int protocol) { message(json,message,protocol); }
}
