package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

/**
 * NO-OP.  Used by the HUD to "check in", payloads may be attached to the response (should be) which will make this make sense (see titler updates).
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class NOOP {

	@Commands(context = Context.CHARACTER, description = "No-operation, used by HUD to poll server", permitConsole = false, permitUserWeb = false)
	public static Response noop(State st) {
		return new JSONResponse(new JSONObject());
	}

}
