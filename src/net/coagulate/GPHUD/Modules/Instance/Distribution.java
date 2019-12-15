package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class Distribution {

	@Nonnull
	@Command.Commands(description = "Get a new Region Server",requiresPermission = "Instance.ServerOperator",permitScripting = false,context = Command.Context.AVATAR,permitObject = false)
	public static Response getServer(@Nonnull State st) {
		try {
			JSONObject json = new JSONObject();
			json.put("incommand", "broadcast");
			json.put("subcommand", "giveitemprefix");
			json.put("itemtogive", "GPHUD Region Server");
			json.put("givetoname", st.getAvatar().getName());
			json.put("giveto", st.getAvatar().getUUID());
			Region.find("Cerasi",false).sendServerSync(json);
		}
		catch (UserException e) {
			throw new UserException("Failed to reach distribution server, please try again in a minute, otherwise wait an hour or two as the region may be under maintenance ["+e.getLocalizedMessage()+"]");
		}
		return new OKResponse("A new region server should be en route to you from the master server");
	}
	@Nonnull
	@Command.Commands(description = "Get a new Remote Dispenser",requiresPermission = "Instance.ServerOperator",permitScripting = false,context = Command.Context.AVATAR,permitObject = false)
	public static Response getDispenser(@Nonnull State st) {
		JSONObject json = new JSONObject();
		json.put("incommand", "servergive");
		json.put("itemname", "GPHUD Remote Dispenser");
		json.put("giveto", st.getAvatar().getUUID());
		st.getRegion().sendServer(json);
		return new OKResponse("A remote dispenser is being set to you by your region server");
	}

}
