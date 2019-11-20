package net.coagulate.GPHUD.Modules.Objects;

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

public class Connect {

	@Command.Commands(description = "Connects to the system as an object",context = Command.Context.AVATAR,permitConsole = false,permitUserWeb = false,permitScripting = false,permitJSON = false)
	public static Response connect(State st) {
		if (!st.hasPermission("Objects.Connect")) {
			Audit.audit(true,st, Audit.OPERATOR.AVATAR,null,null,"REJECTED","Connection","","","Rejected GPHUD Object connection from "+st.sourcename+" at "+st.sourceregion+"/"+st.sourcelocation);
			return new TerminateResponse("You do not have permissions to connect objects at this instance!");
		}
		int version= Interface.convertVersion(st.json.getString("version"));
		int maxversion=Objects.getMaxVersion();
		Objects oldobject=Objects.findOrNull(st,st.objectkey);
		Objects obj= Objects.connect(st,st.objectkey,st.sourcename,st.getRegion(),st.sourceowner,st.sourcelocation,st.callbackurl,version);
		if (oldobject==null) {
			Audit.audit(true,st, Audit.OPERATOR.AVATAR,null,null,"New","Connection","",st.sourcename,"Conected new object at "+st.sourceregion+"/"+st.sourcelocation);
		}
		String versionsuffix="";
		if (maxversion>version) { versionsuffix=" (An updated version of this script is available)"; }
		if (version>maxversion) { versionsuffix=" (Well, hello new version)"; }
		JSONObject response=new JSONObject();
		response.put("incommand","registered");
		ObjectTypes objecttype = obj.getObjectType();
		String behaviour="No behaviour is mapped";
		if (objecttype!=null) {
			ObjectType ot = ObjectType.materialise(st, objecttype);
			behaviour= ot.explainText();
			behaviour+="\nOperating mode: "+ot.mode();
			ot.payload(st,response);
		}
		if (!st.json.has("silent")) {
			response.put("message","Registered object#"+obj.getId()+"\nName: "+obj.getName()+"\nOwner: "+st.getAvatar()+"\nCharacter: "+st.getCharacterNullable()+"\nVersion: "+version+versionsuffix+"\nBehaviour: "+behaviour);
		};
		return new JSONResponse(response);
	}
}
