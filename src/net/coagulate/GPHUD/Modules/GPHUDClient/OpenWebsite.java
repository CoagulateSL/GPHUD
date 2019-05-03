package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Data.Cookies;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.Responses.JSONPushResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
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
public class OpenWebsite {

	@Commands(context = Context.CHARACTER, description = "Causes the GPHUD to send an llOpenURL to the user directing them to SSO to the Main Website", permitUserWeb = false, permitHUDWeb = false, permitConsole = false)
	public static Response openWebsite(State st) {
		JSONObject json = new JSONObject();
		json.put("incommand", "openurl");
		//String cookie=st.cookiestring; // dont use the same cookie cos the user could log out the session which would nerf the hud's web panel
		String cookie = Cookies.generate(st.avatar(), st.getCharacter(), st.getInstance(), true);
		json.put("url", Interface.generateURL(st, "?gphud=" + cookie));
		json.put("description", "Open GPHUD Administrative Application");
		//System.out.println("OPENURL:"+json.toString());
		return new JSONPushResponse(json, st.getCharacter().getURL(), new OKResponse("Request sent"));
	}

	@Commands(context = Context.ANY, description = "Causes the GPHUD to send an llOpenURL to the user with a custom URL/description", permitUserWeb = false, permitHUDWeb = false, permitConsole = true)
	public static Response offerWebsite(State st,
	                                    @Argument.Arguments(description = "URL to offer to user", type = Argument.ArgumentType.TEXT_ONELINE, max = 255)
			                                    String url,
	                                    @Argument.Arguments(description = "Description to offer with the URL", type = Argument.ArgumentType.TEXT_MULTILINE, max = 254)
			                                    String description) {
		JSONObject json = new JSONObject();
		json.put("incommand", "openurl");
		//String cookie=st.cookiestring; // dont use the same cookie cos the user could log out the session which would nerf the hud's web panel
		String cookie = Cookies.generate(st.avatar(), st.getCharacter(), st.getInstance(), true);
		json.put("url", url);
		json.put("description", description);
		//System.out.println("OPENURL:"+json.toString());
		return new JSONPushResponse(json, st.getCharacter().getURL(), new OKResponse("Request sent"));
	}
}
