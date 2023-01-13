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
	
	/**
	 * Create a new JSON Response based around a JSONObject
	 *
	 * @param j Base JSON object
	 */
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
	
	/**
	 * Target will emit message .. not really sure why this differs from sayashud at this point
	 *
	 * @param json     Existing JSON Object to package the say into
	 * @param message  Message to say
	 * @param protocol Say-Protocol version for the receiving object
	 */
	public static void say(@Nonnull final JSONObject json,@Nonnull final String message,final int protocol) {
		sayAs(json,null,message,protocol);
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
	
	/**
	 * Target will emit message under guise of the sayAs name
	 *
	 * @param json     JSON Object to insert the sayAs into
	 * @param sayAs    Target to say as, may be null to just say as the object.
	 * @param message  Message to have the target sayAs
	 * @param protocol Protocol version for the recieving object
	 */
	public static void sayAs(@Nonnull final JSONObject json,
	                         @Nullable final String sayAs,
	                         @Nonnull final String message,
	                         final int protocol) {
		if (message.isEmpty()) {
			return;
		}
		if (protocol<3) {
			if (sayAs!=null) {
				json.put("sayas",sayAs);
			}
			json.put(findFreePrefix(json,"say"),message);
		} else {
			if (sayAs==null) {
				json.put(findFreePrefix(json,"output"),"s"+message);
			} else {
				json.put(findFreePrefix(json,"output"),"a"+sayAs+"|"+message);
			}
		}
	}
	
	/**
	 * Finds a free message prefix for a given message type.
	 * <p>
	 * I.e. this method is used to find sayas, sayas1, sayas2 next free slot
	 *
	 * @param json   JSON Object to search through
	 * @param prefix Prefix we're interested in
	 * @return The unique next in sequence prefixed string, e.g. sayas2
	 */
	public static String findFreePrefix(@Nonnull final JSONObject json,@Nonnull final String prefix) {
		int i=1;
		while (json.has(prefix+i)) {
			i++;
		}
		return prefix+i;
	}
	
	@Nonnull
	@Override
	public String asHtml(final State st,final boolean rich) {
		throw new SystemImplementationException("JSONResponse can not be converted to HTML");
	}
	
	/**
	 * Target will emit message .. not really sure why this differs from sayashud at this point
	 *
	 * @param message  Message to say
	 * @param protocol Say-Protocol version for the receiving object
	 */
	public void say(@Nonnull final String message,final int protocol) {
		sayAs(null,message,protocol);
	}
	
	/**
	 * Target will emit message under guise of the sayAs name
	 *
	 * @param sayAs    Name to say as, null to just say as objects default name.
	 * @param message  Message to say
	 * @param protocol Say-Protocol version for the receiving object
	 */
	public void sayAs(@Nullable final String sayAs,@Nonnull final String message,final int protocol) {
		sayAs(json,sayAs,message,protocol);
	}
	
	/**
	 * Target will emit message as the GPHUD
	 *
	 * @param message  Message to say
	 * @param protocol Say-Protocol version for the receiving object
	 */
	public void sayAsHud(@Nonnull final String message,final int protocol) {
		sayAsHud(json,message,protocol);
	}
	
	/**
	 * Target will emit message as the GPHUD
	 *
	 * @param json     Existing JSON Object to package the say into
	 * @param message  Message to say
	 * @param protocol Say-Protocol version for the receiving object
	 */
	public static void sayAsHud(@Nonnull final JSONObject json,@Nonnull final String message,final int protocol) {
		if (message.isEmpty()) {
			return;
		}
		if (protocol<3) {
			json.put(findFreePrefix(json,"sayashud"),message);
		} else {
			json.put(findFreePrefix(json,"output"),"s"+message);
		}
	}
	
	/**
	 * Target will message the owner (does not work on objects)
	 *
	 * @param message  Message to say
	 * @param protocol Say-Protocol version for the receiving object
	 */
	public void ownerSay(@Nonnull final String message,final int protocol) {
		ownerSay(json,message,protocol);
	}
	
	/**
	 * Target will message the owner (does not work on objects)
	 *
	 * @param json     Existing JSON Object to package the say into
	 * @param message  Message to say
	 * @param protocol Say-Protocol version for the receiving object
	 */
	public static void ownerSay(@Nonnull final JSONObject json,@Nonnull final String message,final int protocol) {
		if (message.isEmpty()) {
			return;
		}
		if (protocol<3) {
			json.put(findFreePrefix(json,"message"),message);
		} else {
			json.put(findFreePrefix(json,"output"),"o"+message);
		}
	}
	
	/**
	 * Target will message the owner (does not work on objects) (alias for ownerSay
	 *
	 * @param message  Message to say
	 * @param protocol Say-Protocol version for the receiving object
	 */
	public void message(@Nonnull final String message,final int protocol) {
		message(json,message,protocol);
	}
	
	/**
	 * Target will message the owner (does not work on objects) (alias for ownerSay)
	 *
	 * @param json     Existing JSON Object to package the say into
	 * @param message  Message to say
	 * @param protocol Say-Protocol version for the receiving object
	 */
	public static void message(@Nonnull final JSONObject json,@Nonnull final String message,final int protocol) {
		ownerSay(json,message,protocol);
	}
}
