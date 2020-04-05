package net.coagulate.GPHUD.Modules.External;

import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class BasicCommands {
	// ---------- STATICS ----------
	@Commands(description="Get a JSON formatted breakdown of this connection's status",
	          permitScripting=false,
	          permitObject=false,
	          permitExternal=true,
	          permitConsole=false,
	          context=Context.ANY,
	          permitUserWeb=false,
	          permitJSON=false)
	@Nonnull
	public static Response status(@Nonnull final State st) {
		JSONObject json=new JSONObject();
		json.put("environment",(GPHUD.DEV?"DEVELOPMENT":"Production"));
		json.put("nodename",Interface.getNode());
		json.put("avatar",st.getAvatarNullable());
		json.put("character",st.getCharacterNullable());
		json.put("instance",st.getInstanceNullable());
		json.put("region",st.getRegionNullable());
		json.put("zone",st.zone);
		json.put("sourcename",st.getSourcenameNullable());
		json.put("sourceowner",st.getSourceownerNullable());
		json.put("sourcedeveloper",st.getSourcedeveloperNullable());
		return new JSONResponse(json);
	}

	@Commands(description="Gets your developer key",
	          permitUserWeb=false,
	          permitJSON=false,
	          permitExternal=false,
	          permitObject=false,
	          permitScripting=false,
	          context=Context.AVATAR)
	@Nonnull
	public static Response getDeveloperKey(@Nonnull final State st) {
		if (!st.getAvatar().hasDeveloperKey()) {
			return new ErrorResponse("You have no developer key, please contact Iain Maltz if you believe this to be in error");
		}
		return new OKResponse("Your developer key is: "+st.getAvatar().getDeveloperKey());
	}
}
