package net.coagulate.GPHUD.Modules.Teleportation;

import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Landmarks;
import net.coagulate.GPHUD.Data.Region;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.JSONResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.State;
import org.apache.http.Header;
import org.json.JSONObject;

import java.util.Set;

public class TeleportCommands {

	@Command.Commands(description = "Teleport the player to a given X, Y and Z",permitUserWeb = false,context = Command.Context.CHARACTER,permitConsole = false)
	public static Response teleportTo(State st,
	                           @Argument.Arguments(description = "Region to teleport to (must be part of the instance",type = Argument.ArgumentType.REGION)
	                           Region region,
	                           @Argument.Arguments(description = "X co-ordinate",type = Argument.ArgumentType.FLOAT,max = 256)
	                           Float x,
	                           @Argument.Arguments(description = "Y co-ordinate",type = Argument.ArgumentType.FLOAT,max = 256)
	                           Float y,
	                           @Argument.Arguments(description = "Z co-ordinate",type = Argument.ArgumentType.FLOAT,max = 4096)
	                           Float z) {
		JSONObject response=new JSONObject();
		String teleportto=region.getGlobalCoordinates()+"|";
		teleportto+="<"+x+","+y+","+z+">|";
		teleportto+="<128,256,0>";
		response.put("teleport",teleportto);
		Audit.audit(st, Audit.OPERATOR.CHARACTER,null,null,"Move","Avatar","",x+","+y+","+z,"Player teleported to "+x+","+y+","+z);
		return new JSONResponse(response);
	}

	@Command.Commands(description="Creates a landmark at the current location",context = Command.Context.CHARACTER,permitUserWeb = false,permitScripting = false,requiresPermission = "Teleportation.CreateLandmark")
	public static Response createLandmark(State st,
	                                      @Argument.Arguments(description = "Name for the landmark, replaces it if it already exists",max = 64,type = Argument.ArgumentType.TEXT_ONELINE)
	                                      String name) {
		String position=null;
		String rotation=null;
		for (Header h:st.headers) {
			if (h.getName().equalsIgnoreCase("X-SecondLife-Local-Position")) { position=h.getValue(); }
			if (h.getName().equalsIgnoreCase("X-SecondLife-Local-Rotation")) { rotation=h.getValue(); }
		}
		position=position.replaceAll("\\(","").replaceAll("\\)","");
		rotation=rotation.replaceAll("\\(","").replaceAll("\\)","");
		String xyz[]=position.split(",");
		String xyzs[]=rotation.split(",");

		float x=Float.parseFloat(xyz[0]);
		float y=Float.parseFloat(xyz[1]);
		float z=Float.parseFloat(xyz[2]);
		float angle= (float) (-Math.asin(Float.parseFloat(xyzs[2]))*((float)2));

		float projectx= (float) Math.cos(angle);
		float projecty= -(float) Math.sin(angle);

		if (position==null || rotation==null) { throw new UserException("Unable to calculate your location/rotation information"); }

		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Set",name,"",x+","+y+","+z,"Created landmark "+name+" at "+x+","+y+","+z+" look at "+projectx+","+projecty);

		Landmarks.create(st.getRegion(),name,x,y,z,x+projectx,y+projecty,z+((float)1));
		return new OKResponse("Landmark created in "+st.getRegion().getName()+" at "+x+","+y+","+z+" looking at "+(x+projectx)+","+(y+projecty));
	}

	@Command.Commands(description="Remove a landmark by name",context = Command.Context.AVATAR,requiresPermission = "Teleportation.DeleteLandmark")
	public static Response deleteLandmark(State st,
	                                      @Argument.Arguments(description = "Landmark name to remove",type = Argument.ArgumentType.TEXT_ONELINE,max = 64)
	                                      String name) {
		Landmarks landmark=Landmarks.find(st.getInstance(),name);
		if (landmark==null) { return new ErrorResponse("Can not delete landmark "+name+" - it does not exist"); }
		Landmarks.obliterate(st.getInstance(),name);
		Audit.audit(st, Audit.OPERATOR.AVATAR,null,null,"Delete",name,"","","Deleted landmark "+name);
		return new OKResponse("Deleted landmark "+name);
	}

	@Command.Commands(description = "Teleport to a landmark", context = Command.Context.CHARACTER,permitUserWeb = false,permitConsole = false)
	public static Response go(State st,
								@Argument.Arguments(description = "Landmark name to teleport to",type = Argument.ArgumentType.TEXT_ONELINE,max = 64)
								String landmark) {
		Landmarks lm=st.getInstance().getLandmark(landmark);
		if (lm==null) { return new ErrorResponse("No landmark named '"+landmark+"'"); }
		JSONObject tp=new JSONObject();
		tp.put("teleport",lm.getHUDRepresentation(false));
		Audit.audit(true,st, Audit.OPERATOR.CHARACTER,null,null,"Move",st.getCharacter().getName(),"",landmark,"Player teleported to "+landmark+" at "+lm.getRegion(true).getName()+":"+lm.getCoordinates()+" lookat "+lm.getLookAt());
		return new JSONResponse(tp);
	}

	public static DropDownList getDropDownList(State st,String name,String selected) {
		DropDownList list=new DropDownList(name);
		Set<Landmarks> landmarks = Landmarks.getAll(st.getInstance());
		for (Landmarks landmark:landmarks) {
			list.add(landmark.getName());
		}
		list.setValue(selected);
		return list;
	}
}
