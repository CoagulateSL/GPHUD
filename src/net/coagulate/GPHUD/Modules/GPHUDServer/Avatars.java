package net.coagulate.GPHUD.Modules.GPHUDServer;

import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

import java.util.Set;

import static java.util.logging.Level.WARNING;

/**
 * Register a script as the 'region server'.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Avatars {
	@Commands(context = Context.AVATAR, permitScripting = false, description = "Synchronise the avatars status with the regions contents, server use only.", permitConsole = false, permitUserWeb = false)
	public static Response setRegionAvatars(State st,
	                                        @Arguments(description = "Comma separated list of avatar key=names on the sim", type = ArgumentType.TEXT_ONELINE, max = 65536)
			                                        String userlist) throws UserException {

		// check authorisation, servers can only be deployed by the instance owner...
		if (st.sourcedeveloper.getId() != 1) {
			return new ErrorResponse("Invalid developer source for priviledged call.");
		}
		Region region = st.getRegion();
		if (!region.getURL().equals(st.callbackurl)) {
			return new ErrorResponse("Invalid callback URL, you do not match the registered region server");
		}
		if (userlist == null) { userlist = ""; }
		Set<User> openvisits = region.getAvatarOpenVisits();

		for (String element : userlist.split(",")) {
			//System.out.println(element);
			String p[] = element.split("=");
			if (p.length == 2) {
				try {
					User thisavi = User.findOrCreateAvatar(p[1], p[0]);
					// we DONT init visits this way =)  character registration does
					openvisits.remove(thisavi);
				} catch (Exception e) {
					st.logger().log(WARNING, "Avatar joiner registration failed, ", e);
				}
			}
		}
		region.departingAvatars(st, openvisits);
		Instance instance = st.getInstance();
		//instance.updateStatus();
		JSONObject json=new JSONObject();
		json.put("autoattach",st.getKV("gphudserver.autoattach"));
		json.put("parcelonly",st.getKV("gphudserver.parcelonly"));
		return new JSONResponse(json);
	}

}
