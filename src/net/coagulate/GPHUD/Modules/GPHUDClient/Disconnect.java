package net.coagulate.GPHUD.Modules.GPHUDClient;

import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Interfaces.Responses.NoResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Disconnect {

	// ---------- STATICS ----------
	@Commands(description="Disconnects a URL from GPHUD",
	          context=Context.CHARACTER,
	          permitExternal=false,
	          permitObject=false,
	          permitScripting=false,
	          permitUserWeb=false,
	          permitConsole=false)
	@Nonnull
	public static Response disconnect(
			@Nonnull
			final State state,
			@Arguments(description="URL to disconnect",
			           type=ArgumentType.TEXT_ONELINE,
			           max=255,
			           mandatory=false)
			@Nullable
			final String url) {
		if (url==null || url.isEmpty()) {
			return new NoResponse();
		}
		Char.disconnectURL(url);
		return new NoResponse();
	}
}
