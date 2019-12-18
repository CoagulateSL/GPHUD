package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Data.Cookies;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * Hook that causes the HUD to close the web panel.
 * Usually called from the hud's web panel, so we have to call back to talk to the actual in world object.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class OpenWebsite {

	@Nonnull
	@Commands(context=Context.CHARACTER, description="Causes the GPHUD to send an llOpenURL to the user directing them to SSO to the Main Website", permitUserWeb=false, permitObject=false)
	public static Response openWebsite(@Nonnull final State st) {
		final JSONObject json=new JSONObject();
		json.put("incommand","openurl");
		//String cookie=st.cookiestring; // dont use the same cookie cos the user could log out the session which would nerf the hud's web panel
		final String cookie=Cookies.generate(st.getAvatarNullable(),st.getCharacter(),st.getInstance(),true);
		json.put("openurl",Interface.generateURL(st,"?gphud="+cookie));
		json.put("description","Open GPHUD Administrative Application");
		//System.out.println("OPENURL:"+json.toString());
		return new JSONResponse(json);
	}

	@Nonnull
	@Commands(context=Context.ANY, description="Causes the GPHUD to send an llOpenURL to the user with a custom URL/description", permitUserWeb=false, permitObject=false)
	public static Response offerWebsite(@Nonnull final State st,
	                                    @Argument.Arguments(description="URL to offer to user", type=Argument.ArgumentType.TEXT_ONELINE, max=255) final String url,
	                                    @Argument.Arguments(description="Description to offer with the URL", type=Argument.ArgumentType.TEXT_MULTILINE, max=254) final String description)
	{
		final JSONObject json=new JSONObject();
		json.put("incommand","openurl");
		//String cookie=st.cookiestring; // dont use the same cookie cos the user could log out the session which would nerf the hud's web panel
		final String cookie=Cookies.generate(st.getAvatarNullable(),st.getCharacter(),st.getInstance(),true);
		json.put("openurl",url);
		json.put("description",description);
		//System.out.println("OPENURL:"+json.toString());
		return new JSONResponse(json);
	}
}
