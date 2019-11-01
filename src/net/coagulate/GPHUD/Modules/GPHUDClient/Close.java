package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

/**
 * Hook that causes the HUD to close the web panel.
 * Usually called from the hud's web panel, so we have to call back to talk to the actual in world object.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Close {
	@Commands(context = Context.CHARACTER, permitScripting = false, permitConsole = false, permitJSON = false, permitUserWeb = false, description = "Causes the HUD to close its web panel")
	public static Response close(State st) {
		//System.out.println("Send close");
		if (st.getCharacter().getURL() == null) {
			return new ErrorResponse("Unable to close, your callback is unavailable.");
		}
		JSONObject close = new JSONObject().put("incommand", "close");
		Transmission t = new Transmission(st.getCharacter(), close);
		t.start();
		return new OKResponse("Closing");
	}
}
