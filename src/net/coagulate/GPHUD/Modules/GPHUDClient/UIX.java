package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Interfaces.Responses.NoResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class UIX {

	// ---------- STATICS ----------
	@Nonnull
	@Commands(description="UIX invoking method, HUD use only",
	          permitUserWeb=false,
	          permitScripting=false,
	          permitObject=false,
	          permitExternal=false,
	          permitConsole=false,
	          context=Context.ANY)
	public static Response call(@Nonnull final State st,
	                            @Arguments(description="Selected option",
	                                       max=128,
	                                       type=ArgumentType.TEXT_ONELINE) @Nonnull final String commandtoinvoke) {
		if (commandtoinvoke.contains(" ")) { return Modules.run(st,commandtoinvoke,false); }
		// we just ignore effects clicks that happen when effects is disabled ...  bit of a bodge really :P
		if (commandtoinvoke.toLowerCase().startsWith("effects.")) {
			if (!Modules.get(null,"Effects").isEnabled(st)) {
				// no-op then
				return new NoResponse();
			}
		}
		if (Modules.getCommand(st,commandtoinvoke).getArguments().size()==0) {
			//argh, it's argless cap'n
			return Modules.run(st,commandtoinvoke,false);
		}
		return Modules.getJSONTemplateResponse(st,commandtoinvoke);
	}
}
