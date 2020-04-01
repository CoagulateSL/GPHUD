package net.coagulate.GPHUD.Modules.Objects;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Tools.MailTools;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.ObjectTypes;
import net.coagulate.GPHUD.Data.Objects;
import net.coagulate.GPHUD.Interface;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.Responses.TerminateResponse;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Objects.ObjectTypes.ObjectType;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;

public class Connect {

	// ---------- STATICS ----------
	@Nonnull
	@Command.Commands(description="Connects to the system as an object",
	                  context=Command.Context.AVATAR,
	                  permitConsole=false,
	                  permitUserWeb=false,
	                  permitScripting=false,
	                  permitJSON=false)
	public static Response connect(@Nonnull final State st) {
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
			            "Rejected GPHUD Object connection from "+st.getSourcename()+" at "+st.sourceregion+"/"+st.sourcelocation
			           );
			return new TerminateResponse("You do not have permissions to connect objects at this instance!");
		}
		// require a callback url
		if (st.callbackurlNullable()==null || st.callbackurl().isEmpty()) {
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
		if (st.objectkey==null) { return new TerminateResponse("No Object Key is present"); }
		if (st.sourcelocation==null) { return new TerminateResponse("No Source Location is present"); }
		final int version=Interface.convertVersion(st.json().getString("version"));
		final int maxversion=Objects.getMaxVersion();
		final Objects oldobject=Objects.findOrNull(st,st.objectkey);
		final Objects obj=Objects.connect(st,st.objectkey,st.getSourcename(),st.getRegion(),st.getSourceowner(),st.sourcelocation,st.callbackurl(),version);
		if (oldobject==null) {
			Audit.audit(true,st,Audit.OPERATOR.AVATAR,null,null,"New","Connection","",st.getSourcename(),"Conected new object at "+st.sourceregion+"/"+st.sourcelocation);
		}
		String versionsuffix="";
		if (maxversion>version) { versionsuffix=" (An updated version of this script is available)"; }
		if (version>maxversion) { versionsuffix=" (Well, hello new version)"; }
		final JSONObject response=new JSONObject();
		response.put("incommand","registered");
		final ObjectTypes objecttype=obj.getObjectType();
		String behaviour="No behaviour is mapped";
		if (objecttype!=null) {
			final ObjectType ot=ObjectType.materialise(st,objecttype);
			behaviour=ot.explainText();
			behaviour+="\nOperating mode: "+ot.mode();
			ot.payload(st,response);
		}
		if (!st.json().has("silent")) {
			response.put("message",
			             "Registered object#"+obj.getId()+"\nName: "+obj.getName()+"\nOwner: "+st.getAvatarNullable()+"\nCharacter: "+st.getCharacterNullable()+"\nVersion: "+version+versionsuffix+"\nBehaviour: "+behaviour
			            );
		}
		return new JSONResponse(response);
	}
}
