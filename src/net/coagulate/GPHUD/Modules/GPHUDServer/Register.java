package net.coagulate.GPHUD.Modules.GPHUDServer;

import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.EndOfLifing;
import net.coagulate.GPHUD.GPHUD;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.TerminateResponse;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.Modules.Command.Commands;
import net.coagulate.GPHUD.Modules.Command.Context;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Config;
import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

/**
 * Register a script as the 'region server'.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Register {

	// ---------- STATICS ----------
	@Nonnull
	@Commands(context=Context.AVATAR,
	          permitScripting=false,
	          permitConsole=false,
	          permitUserWeb=false,
	          description="Registers this connection as the region server "+"connection",
	          requiresPermission="Instance.ServerOperator",
	          permitObject=false,
	          permitExternal=false)
	public static Response register(@Nonnull final State st,
	                                @Nonnull @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                                    name="version",description="Version number of the Server that is connecting",
	                                                    max=64) final String version,
	                                @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                           name="versiondate",description="Version date of the Server that is connecting",
	                                           max=64) final String versiondate,
	                                @Arguments(type=ArgumentType.TEXT_ONELINE,
	                                           name="versiontime",description="Version time of the Server that is connecting",
	                                           max=64) final String versiontime) {
		if (EndOfLifing.hasExpired(version)) {
			st.logger().warning("Rejected region server connection from end-of-life product version "+version+" from "+versiondate+" "+versiontime);
			return new TerminateResponse("Sorry, this Region Server is so old it is no longer supported.\nPlease tell your sim administrator to deploy an update.");
		}
		// check authorisation, servers can only be deployed by the instance owner...
		final String regionName=st.getRegionName();
		final Instance instance=st.getInstance();
		/* -- NOW obsoleted and actually uses the permissions checking code (?)
		if (st.avatar() != instance.getOwner() && st.avatar().isSuperAdmin()==false) {
			st.logger().log(WARNING, "Not the instance owner (who is " + instance.getOwner() + ")");
			return new TerminateResponse("You are not the instance owner, you can not deploy server nodes");
		}
		 */
		final Region region=st.getRegion();
		for (final Header header: st.req().getAllHeaders()) {
			if ("X-SecondLife-Region".equalsIgnoreCase(header.getName())) {
				//System.out.println("Element: "+header.getValue());
				final Matcher match = Pattern.compile("^.* \\(([0-9]+), ([0-9]+)\\)$").matcher(header.getValue());
				if (match.matches()) {
					region.setGlobalCoordinates(Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2)));
				} else {
					st.logger().log(WARNING, "Failed to parse region format: " + header.getValue());
				}
			}
		}
		String url=null;
		try { url=st.json().getString("callback"); } catch (@Nonnull final JSONException ignored) {}
		if (url==null || "".equals(url)) {
			st.logger().log(WARNING,"No callback URL sent to GPHUDClient.Register");
			return new ErrorResponse("You are not set up with a callback URL");
		}
		region.setURL(url);
		if (st.objectKey !=null) { region.setPrimUUID(st.objectKey); }
		st.logger().log(INFO,"Sending post registration message to "+regionName);
		final JSONObject registered=new JSONObject().put("incommand","registered");
		String registrationMessage;
		registrationMessage=GPHUD.serverVersion()+" [https://"+ Config.getURLHost()+"/GPHUD/ChangeLog Change Log]"+GPHUD.brandingWithNewline();
		if (st.getRegion().needsUpdate()) {
			registrationMessage += """

					=====
					Update required: This GPHUD Region Server is out of date.  If you are the instance owner, please attach a HUD to be sent a new version.
					=====""";
		}
		JSONResponse.message(registered,registrationMessage,region.protocol());
		final Transmission t=new Transmission(region,registered);
		t.start();
		final JSONObject j=new JSONObject();
		region.recordServerVersion(st,version,versiondate,versiontime,st.protocol);
		j.put("incommand","registering");
		j.put("instancename",instance.getName());
		j.put("autoattach",st.getKV("GPHUDServer.AutoAttach"));
		j.put("parcelonly",st.getKV("GPHUDServer.ParcelONLY"));
		j.put("setlogo",st.getKV("GPHUDClient.logo"));
		j.put("minz",st.getKV("gphudserver.dispenserminimumz"));
		j.put("maxz",st.getKV("gphudserver.dispensermaximumz"));
		instance.updateStatus();
		return new JSONResponse(j);
	}

	public static void sendAttachConfig(@Nonnull final State st) {
		final JSONObject j=new JSONObject();
		j.put("autoattach",st.getKV("GPHUDServer.AutoAttach"));
		j.put("parcelonly",st.getKV("GPHUDServer.ParcelONLY"));
		j.put("minz",st.getKV("gphudserver.dispenserminimumz"));
		j.put("maxz",st.getKV("gphudserver.dispensermaximumz"));
		st.getInstance().sendServers(j);
	}
}
