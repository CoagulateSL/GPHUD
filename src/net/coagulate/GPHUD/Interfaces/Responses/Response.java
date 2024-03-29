package net.coagulate.GPHUD.Interfaces.Responses;

import net.coagulate.GPHUD.Interfaces.Outputs.Renderable;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

/**
 * An interface independant Response format from commands.
 * Can be rendered into JSON or HTML.
 * Default implementation uses a "message=" format for JSON (or can be overridden in subclasses).
 * HTML returns merely the message, bolding it if its not just a "message=".
 *
 * @author Iain Price <gphud@predestined.net>
 */
public interface Response extends Renderable {
	// ---------- INSTANCE ----------
	JSONObject asJSON(State st);
	
	String scriptResponse();
}
