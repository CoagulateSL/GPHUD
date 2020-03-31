package net.coagulate.GPHUD.Modules.GPHUDClient;


import net.coagulate.Core.Exceptions.User.UserConfigurationException;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * Demonstration for HUD "quick buttons"
 * These should all eventually be generic and configurable by the instance, but for now, here's a demo implementation.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class QuickButtons {

	// ---------- STATICS ----------
	@Commands(description="Triggered when quick button 1 is pressed",
	          permitConsole=false,
	          permitUserWeb=false,
	          context=Context.CHARACTER,
	          permitObject=false,
	          permitScripting=false)
	public static Response quickButton1(@Nonnull final State st) {
		return quickButton(st,1);
	}

	@Commands(description="Triggered when quick button 2 is pressed",
	          permitConsole=false,
	          permitUserWeb=false,
	          context=Context.CHARACTER,
	          permitObject=false,
	          permitScripting=false)
	public static Response quickButton2(@Nonnull final State st) {
		return quickButton(st,2);
	}

	@Commands(description="Triggered when quick button 3 is pressed",
	          permitConsole=false,
	          permitUserWeb=false,
	          context=Context.CHARACTER,
	          permitObject=false,
	          permitScripting=false)
	public static Response quickButton3(@Nonnull final State st) {
		return quickButton(st,3);
	}

	@Commands(description="Triggered when quick button 4 is pressed",
	          permitConsole=false,
	          permitUserWeb=false,
	          context=Context.CHARACTER,
	          permitObject=false,
	          permitScripting=false)
	public static Response quickButton4(@Nonnull final State st) {
		return quickButton(st,4);
	}

	@Commands(description="Triggered when quick button 5 is pressed",
	          permitConsole=false,
	          permitUserWeb=false,
	          context=Context.CHARACTER,
	          permitObject=false,
	          permitScripting=false)
	public static Response quickButton5(@Nonnull final State st) {
		return quickButton(st,5);
	}

	@Commands(description="Triggered when quick button 6 is pressed",
	          permitConsole=false,
	          permitUserWeb=false,
	          context=Context.CHARACTER,
	          permitObject=false,
	          permitScripting=false)
	public static Response quickButton6(@Nonnull final State st) {
		return quickButton(st,6);
	}

	// ----- Internal Statics -----
	static Response quickButton(@Nonnull final State st,
	                            final int button) {
		final String commandname=st.getKV("GPHUDClient.QuickButton"+button).value();
		if (commandname==null || commandname.isEmpty()) { return new JSONResponse(new JSONObject()); }
		if (commandname.toLowerCase().startsWith("gphudclient.quickbutton")) {
			throw new UserConfigurationException("Quick button "+button+" is not permitted to call another quick button ("+commandname+")");
		}
		return templateOrRun(st,commandname);
	}

	static Response templateOrRun(@Nonnull final State st,
	                              @Nonnull final String command) {
		final JSONObject template=Modules.getJSONTemplate(st,command);
		if (template.getInt("args")==0) { return Modules.run(st,command,new SafeMap()); }
		return new JSONResponse(template);
	}
}
