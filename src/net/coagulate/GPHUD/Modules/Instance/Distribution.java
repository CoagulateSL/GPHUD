package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;

public class Distribution {

	@Command.Commands(description = "Get a new Region Server",requiresPermission = "instance.owner",context = Command.Context.AVATAR)
	public static Response getServer(State st) {
		GPHUD.sendNewServer(st.getAvatar());
		return new OKResponse("A new region server should be en route to you");
	}
	@Command.Commands(description = "Get a new Remote Dispenser",requiresPermission = "instance.owner",context = Command.Context.AVATAR)
	public static Response getDispenser(State st) {
		GPHUD.sendDispenser(st.getAvatar());
		return new OKResponse("A new remote dispenser should be en route to you");
	}

}
