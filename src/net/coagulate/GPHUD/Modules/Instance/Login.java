package net.coagulate.GPHUD.Modules.Instance;

import net.coagulate.GPHUD.Data.Cookies;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class Login {

	@Nonnull
	@Command.Commands(context=Command.Context.AVATAR, description="Causes the GPHUD server to IM the character a URL to the Main Website", permitScripting=false,
	                  permitUserWeb=false, permitObject=false, permitJSON=false)
	public static Response login(@Nonnull final State st) {
		final JSONObject json=new JSONObject();
		json.put("incommand","messageto");
		final String cookie=Cookies.generate(st.getAvatar(),st.getCharacterNullable(),st.getInstance(),true);
		json.put("sendthismessage","["+Interface.generateURL(st,"?gphud="+cookie)+" GPHUD Configuration Site]");
		json.put("target",st.getAvatar().getUUID());
		return new JSONResponse(json);
	}

}