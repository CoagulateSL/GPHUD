package net.coagulate.GPHUD.Modules.GPHUDServer;

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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * Register a script as the 'region server'.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Avatars {
	// ---------- STATICS ----------
	@Nonnull
	@Commands(context=Context.AVATAR,
	          permitScripting=false,
	          description="Synchronise the avatars status with the regions contents, server use only.",
	          permitConsole=false,
	          permitUserWeb=false,
	          permitObject=false,
	          permitExternal=false)
	public static Response setRegionAvatars(@Nonnull final State st,
	                                        @Nullable
	                                        @Arguments(name="userlist",
	                                                   description="Comma separated list of avatar key=names on the sim",
	                                                   type=ArgumentType.TEXT_ONELINE,
	                                                   max=65536,
	                                                   mandatory=false) String userlist) {
		
		// check authorisation, servers can only be deployed by the instance owner...
		if (st.getSourceDeveloper().getId()!=1) {
			return new ErrorResponse("Invalid developer source for priviledged call.");
		}
		final Region region=st.getRegion();
		if (st.objectKey!=null) {
			region.setPrimUUID(st.objectKey);
		}
		/*if (!region.getURL().equals(st.callbackurl())) {
			return new ErrorResponse("Invalid callback URL, you do not match the registered region server");
		}*/
		if (userlist==null) {
			userlist="";
		}
		final Set<User> openvisits=region.getAvatarOpenVisits();
		
		for (final String element: userlist.split(",")) {
			//System.out.println(element);
			final String[] p=element.split("=");
			if (p.length==2) {
				try {
					final User thisavi=User.findOrCreate(p[1].trim(),p[0].trim(),false);
					// we DONT init visits this way =)  character registration does
					openvisits.remove(thisavi);
				} catch (@Nonnull final Exception e) {
					st.logger().log(WARNING,"Avatar joiner registration failed, ",e);
				}
			}
		}
		region.departingAvatars(st,openvisits);
		//final Instance instance = st.getInstance();
		//instance.updateStatus();
		final JSONObject json=new JSONObject();
		json.put("autoattach",st.getKV("gphudserver.autoattach"));
		json.put("parcelonly",st.getKV("gphudserver.parcelonly"));
		json.put("minz",st.getKV("gphudserver.dispenserminimumz"));
		json.put("maxz",st.getKV("gphudserver.dispensermaximumz"));
		if (region.getURLNullable()==null) {
			json.put("rebootserver","rebootserver");
			st.logger().log(SEVERE,"Rebooting region server for "+region+" due to null URL");
		}
		return new JSONResponse(json);
	}
	
}
