package net.coagulate.GPHUD.Modules.Objects;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Data.Obj;
import net.coagulate.GPHUD.Data.ObjType;
import net.coagulate.GPHUD.Interfaces.Interface;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.TerminateResponse;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Objects.ObjectTypes.ObjectType;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Config;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.MessagingException;

public class Connect {

	// ---------- STATICS ----------
	@Nonnull
	@Command.Commands(description="Connects to the system as an object",
	                  context=Command.Context.AVATAR,
	                  permitConsole=false,
	                  permitUserWeb=false,
	                  permitScripting=false,
	                  permitJSON=false,
	                  permitExternal=false)
	public static Response connect(@Nonnull final State st,
								   @Argument.Arguments(description = "Boot parameters",name = "bootparams",type = Argument.ArgumentType.TEXT_ONELINE,max=1024,mandatory=false)
								   @Nullable final String bootParams) {
		if (!st.hasPermission("Objects.Connect")) {
			Audit.audit(true,
			            st,
			            Audit.OPERATOR.AVATAR,
			            null,
			            null,
			            "REJECTED",
			            "Connection",
			            "",
			            "",
			            "Rejected GPHUD Object connection from "+st.getSourceName()+" at "+st.sourceRegion +"/"+st.sourceLocation
			           );
			return new TerminateResponse("You do not have permissions to connect objects at this instance!");
		}
		// require a callback url
		if (st.callBackURLNullable()==null || st.callBackURL().isEmpty()) {
			try {
				MailTools.mail("Callback URL is null or blank, sending reboot",st.toHTML());
			}
			catch (final MessagingException e) {
				throw new SystemImplementationException("Mailout exception",e);
			}
			final JSONObject json=new JSONObject();
			json.put("reboot","No callback URL was presented, server requests us to restart");
			return new JSONResponse(json);
		}
		if (st.objectKey ==null) { return new TerminateResponse("No Object Key is present"); }
		if (st.sourceLocation ==null) { return new TerminateResponse("No Source Location is present"); }
		final int version=Interface.convertVersion(st.json().getString("version"));
		final int maxversion=Obj.getMaxVersion();
		final Obj oldobject=Obj.findOrNull(st,st.objectKey);
		final Obj obj=Obj.connect(st,st.objectKey,st.getSourceName(),st.getRegion(),st.getSourceOwner(),st.sourceLocation,st.callBackURL(),version,bootParams,st.protocol);
		if (oldobject==null) {
			Audit.audit(true,st,Audit.OPERATOR.AVATAR,null,null,"New","Connection","",st.getSourceName(),"Conected new object at "+st.sourceRegion +"/"+st.sourceLocation);
		}
		String versionsuffix="";
		if (maxversion>version) { versionsuffix=" (An updated version of this script is available)"; }
		if (version>maxversion) { versionsuffix=" (Well, hello new version)"; }
		final JSONObject response=new JSONObject();
		response.put("incommand","registered");
		final ObjType objecttype=obj.getObjectType();
		String behaviour="No behaviour is mapped";
		if (objecttype!=null) {
			final ObjectType ot=ObjectType.materialise(st,objecttype);
			behaviour=ot.explainText();
			behaviour+="\nOperating mode: "+ot.mode();
			Char associatedCharacter=ot.getCharacter();
			if (associatedCharacter!=null) { associatedCharacter.setProtocol(st.protocol); }
			if (st.getCharacterNullable()!=null) { st.getCharacter().setProtocol(st.protocol); }
			ot.payload(st,response,st.getRegion(),st.callBackURL());
		}
		behaviour+="\nServicing Node: "+ Config.getHostName();
		if (!st.json().has("silent")) {
			JSONResponse.message(response,
			             "Registered object#"+obj.getId()+"\nName: "+obj.getName()+"\nOwner: "+st.getAvatarNullable()+"\nCharacter: "+st.getCharacterNullable()+"\nVersion: "+version+versionsuffix+"\nBehaviour: "+behaviour
			            );
		}
		return new JSONResponse(response);
	}
}
