package net.coagulate.GPHUD.Modules.ItemGiver;

import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

/**
 * @author Iain Price
 */
public class Giver {

	@Nonnull
	@Commands(context = Command.Context.AVATAR, description = "Get an Item Giver", permitScripting = false, requiresPermission = "Instance.ServerOperator", permitUserWeb = false,permitObject = false)
	public static Response getGiver(@Nonnull State st) {
		JSONObject json = new JSONObject();
		json.put("incommand", "servergive");
		json.put("itemname", "GPHUD Item Giver");
		json.put("giveto", st.getAvatar().getUUID());
		st.getRegion().sendServer(json);
		return new OKResponse("OK - Sent you an Item Giver");
	}

	@Nonnull
	@Commands(context = Command.Context.AVATAR, description = "Get an Item from a giver", permitUserWeb = false,permitObject = false)
	public static Response get(@Nonnull State st,
	                           @Argument.Arguments(description = "Name of object to give to avatar", type = Argument.ArgumentType.TEXT_ONELINE, max = 63)
			                           String item) {
		JSONObject json = new JSONObject();
		json.put("incommand", "broadcast");
		json.put("subcommand", "giveitem");
		json.put("itemtogive", item);
		json.put("giveto", st.getAvatar().getUUID());
		json.put("givetoname", st.getAvatar().getName());
		st.getRegion().sendServer(json);
		return new OKResponse("OK - Request for item has been sent");
	}
}
