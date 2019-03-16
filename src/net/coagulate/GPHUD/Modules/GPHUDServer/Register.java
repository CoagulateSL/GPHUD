package net.coagulate.GPHUD.Modules.GPHUDServer;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Instance;
import net.coagulate.GPHUD.Data.Region;
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
import org.json.JSONException;
import org.json.JSONObject;

/** Register a script as the 'region server'.
 *
* @author Iain Price <gphud@predestined.net>
 */
public abstract class Register {

    @Commands(context = Context.AVATAR,permitConsole = false,permitHUDWeb = false,permitUserWeb = false,description = "Registers this connection as the region server connection")
    public static Response register(State st,
            @Arguments(type = ArgumentType.TEXT_ONELINE,description = "Version number of the Server that is connecting",max=64)
                String version,
            @Arguments(type = ArgumentType.TEXT_ONELINE,description = "Version date of the Server that is connecting",max=64)
                String versiondate,
            @Arguments(type = ArgumentType.TEXT_ONELINE,description = "Version time of the Server that is connecting",max=64)
                String versiontime) throws UserException, SystemException {           
        // check authorisation, servers can only be deployed by the instance owner...
        String regionname=st.getRegionName();
        Instance instance=st.getInstance();
        if (st.avatar()!=instance.getOwner()) {
            st.logger().log(WARNING,"Not the instance owner (who is "+instance.getOwner()+")");
            return new TerminateResponse("You are not the instance owner, you can not deploy server nodes");
        }            
        String url=null;
        try { url=st.json.getString("callback"); } catch (JSONException e) {}
        if (url==null || url.equals("")) { 
            st.logger().log(WARNING,"No callback URL sent with registration");
            return new ErrorResponse("You are not set up with a callback URL");
        }
        Region region=st.getRegion();
        region.setURL(url);
        st.logger().log(INFO,"Sending post registration message to "+regionname);
        JSONObject registered=new JSONObject().put("incommand","registered");
        Transmission t=new Transmission(region,registered);
        t.start();
        JSONObject j=new JSONObject();
        region.recordServerVersion(st,version,versiondate,versiontime);
        j.put("incommand","registering");
        j.put("instancename",instance.getName());
        j.put("autoattach", st.getKV("GPHUDServer.AutoAttach"));
        j.put("parcelonly", st.getKV("GPHUDServer.ParcelONLY"));
        j.put("setlogo",st.getKV("GPHUDClient.logo"));
        instance.updateStatus();
        return new JSONResponse(j);
    }

    public static void sendAttachConfig(State st) {
        JSONObject j=new JSONObject();
        j.put("autoattach", st.getKV("GPHUDServer.AutoAttach"));
        j.put("parcelonly", st.getKV("GPHUDServer.ParcelONLY"));
        st.getInstance().sendServers(j);
    }
}
