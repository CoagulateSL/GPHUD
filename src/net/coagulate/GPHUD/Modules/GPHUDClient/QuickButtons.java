package net.coagulate.GPHUD.Modules.GPHUDClient;


import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.UserException;
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

	@Commands(description = "Triggered when quick button 1 is pressed", permitConsole = false, permitUserWeb = false, context = Context.CHARACTER,permitObject = false,permitScripting = false)
	public static Response quickButton1(@Nonnull State st) throws UserException, SystemException {
		return templateOrRun(st, st.getKV("GPHUDClient.QuickButton1").value());
	}

	@Commands(description = "Triggered when quick button 2 is pressed", permitConsole = false, permitUserWeb = false, context = Context.CHARACTER,permitObject = false,permitScripting = false)
	public static Response quickButton2(@Nonnull State st) throws UserException, SystemException {
		return templateOrRun(st, st.getKV("GPHUDClient.QuickButton2").value());
	}

	@Commands(description = "Triggered when quick button 3 is pressed", permitConsole = false, permitUserWeb = false, context = Context.CHARACTER,permitObject = false,permitScripting = false)
	public static Response quickButton3(@Nonnull State st) {
		return templateOrRun(st, st.getKV("GPHUDClient.QuickButton3").value());
	}

	@Commands(description = "Triggered when quick button 4 is pressed", permitConsole = false, permitUserWeb = false, context = Context.CHARACTER,permitObject = false,permitScripting = false)
	public static Response quickButton4(@Nonnull State st) {
		return templateOrRun(st, st.getKV("GPHUDClient.QuickButton4").value());
	}

	@Commands(description = "Triggered when quick button 5 is pressed", permitConsole = false, permitUserWeb = false, context = Context.CHARACTER,permitObject = false,permitScripting = false)
	public static Response quickButton5(@Nonnull State st) {
		return templateOrRun(st, st.getKV("GPHUDClient.QuickButton5").value());
	}

	@Commands(description = "Triggered when quick button 6 is pressed", permitConsole = false, permitUserWeb = false, context = Context.CHARACTER,permitObject = false,permitScripting = false)
	public static Response quickButton6(@Nonnull State st) throws UserException, SystemException {
		return templateOrRun(st, st.getKV("GPHUDClient.QuickButton6").value());
	}

	static Response templateOrRun(@Nonnull State st, @Nonnull String command) throws UserException, SystemException {
		JSONObject template = Modules.getJSONTemplate(st, command);
		if (template.getInt("args") == 0) { return Modules.run(st, command, new SafeMap()); }
		return new JSONResponse(template);
	}
}
