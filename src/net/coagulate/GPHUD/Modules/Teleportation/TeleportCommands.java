package net.coagulate.GPHUD.Modules.Teleportation;

import net.coagulate.Core.Exceptions.User.UserInputEmptyException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Landmark;
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

import javax.annotation.Nonnull;
import java.util.Set;

public class TeleportCommands {

	// ---------- STATICS ----------
	@Nonnull
	@Command.Commands(description="Teleport the player to a given X, Y and Z",
	                  permitUserWeb=false,
	                  context=Command.Context.CHARACTER,
	                  permitConsole=false,
	                  permitObject=false,
	                  permitExternal=false)
	public static Response teleportTo(@Nonnull final State st,
	                                  @Nonnull @Argument.Arguments(description="Region to teleport to (must be part of the instance",
	                                                               type=Argument.ArgumentType.REGION) final Region region,
	                                  @Argument.Arguments(description="X co-ordinate",
	                                                      type=Argument.ArgumentType.FLOAT,
	                                                      max=256) final Float x,
	                                  @Argument.Arguments(description="Y co-ordinate",
	                                                      type=Argument.ArgumentType.FLOAT,
	                                                      max=256) final Float y,
	                                  @Argument.Arguments(description="Z co-ordinate",
	                                                      type=Argument.ArgumentType.FLOAT,
	                                                      max=4096) final Float z) {
		final JSONObject response=new JSONObject();
		String teleportto=region.getGlobalCoordinates()+"|";
		teleportto+="<"+x+","+y+","+z+">|";
		teleportto+="<128,256,0>";
		response.put("teleport",teleportto);
		Audit.audit(st,Audit.OPERATOR.CHARACTER,null,null,"Move","Avatar","",x+","+y+","+z,"Player teleported to "+x+","+y+","+z);
		return new JSONResponse(response);
	}

	@Nonnull
	@Command.Commands(description="Creates a landmark at the current location",
	                  context=Command.Context.CHARACTER,
	                  permitUserWeb=false,
	                  permitScripting=false,
	                  requiresPermission="Teleportation.CreateLandmark",
	                  permitExternal=false,
	                  permitObject=false)
	public static Response createLandmark(@Nonnull final State st,
	                                      @Argument.Arguments(description="Name for the landmark, replaces it if it already exists",
	                                                          max=64,
	                                                          type=Argument.ArgumentType.TEXT_ONELINE) final String name) {
		String position=null;
		String rotation=null;
		for (final Header h: st.headers()) {
			if (h.getName().equalsIgnoreCase("X-SecondLife-Local-Position")) { position=h.getValue(); }
			if (h.getName().equalsIgnoreCase("X-SecondLife-Local-Rotation")) { rotation=h.getValue(); }
		}
		if (position==null || rotation==null) {
			throw new UserInputEmptyException("Unable to calculate your location/rotation information");
		}
		position=position.replaceAll("\\(","").replaceAll("\\)","");
		rotation=rotation.replaceAll("\\(","").replaceAll("\\)","");
		final String[] xyz=position.split(",");
		final String[] xyzs=rotation.split(",");

		final float x=Float.parseFloat(xyz[0]);
		final float y=Float.parseFloat(xyz[1]);
		final float z=Float.parseFloat(xyz[2]);
		final float angle=(float) (-Math.asin(Float.parseFloat(xyzs[2]))*((float) 2));

		final float projectx=(float) Math.cos(angle);
		final float projecty=-(float) Math.sin(angle);

		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Set",name,"",x+","+y+","+z,"Created landmark "+name+" at "+x+","+y+","+z+" look at "+projectx+","+projecty);

		Landmark.create(st.getRegion(),name,x,y,z,x+projectx,y+projecty,z+((float) 1));
		return new OKResponse("Landmark created in "+st.getRegion().getName()+" at "+x+","+y+","+z+" looking at "+(x+projectx)+","+(y+projecty));
	}

	@Nonnull
	@Command.Commands(description="Remove a landmark by name",
	                  context=Command.Context.AVATAR,
	                  requiresPermission="Teleportation.DeleteLandmark",
	                  permitExternal=false,
	                  permitObject=false,
	                  permitScripting=false)
	public static Response deleteLandmark(@Nonnull final State st,
	                                      @Argument.Arguments(description="Landmark name to remove",
	                                                          type=Argument.ArgumentType.TEXT_ONELINE,
	                                                          max=64) final String name) {
		final Landmark landmark=Landmark.find(st.getInstance(),name);
		if (landmark==null) { return new ErrorResponse("Can not delete landmark "+name+" - it does not exist"); }
		Landmark.obliterate(st.getInstance(),name);
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Delete",name,"","","Deleted landmark "+name);
		return new OKResponse("Deleted landmark "+name);
	}

	@Nonnull
	@Command.Commands(description="Teleport to a landmark",
	                  context=Command.Context.CHARACTER,
	                  permitUserWeb=false,
	                  permitConsole=false,
	                  permitObject=false,
	                  permitExternal=false)
	public static Response go(@Nonnull final State st,
	                          @Argument.Arguments(description="Landmark name to teleport to",
	                                              type=Argument.ArgumentType.TEXT_ONELINE,
	                                              max=64) final String landmark) {
		final Landmark lm=Landmark.find(st,landmark);
		if (lm==null) { return new ErrorResponse("No landmark named '"+landmark+"'"); }
		final JSONObject tp=new JSONObject();
		tp.put("teleport",lm.getHUDRepresentation(false));
		Audit.audit(true,
		            st,
		            Audit.OPERATOR.CHARACTER,
		            null,
		            null,
		            "Move",
		            st.getCharacter().getName(),
		            "",
		            landmark,
		            "Player teleported to "+landmark+" at "+lm.getRegion(true).getName()+":"+lm.getCoordinates()+" lookat "+lm.getLookAt()
		           );
		return new JSONResponse(tp);
	}

	@Nonnull
	public static DropDownList getDropDownList(@Nonnull final State st,
	                                           final String name,
	                                           final String selected) {
		final DropDownList list=new DropDownList(name);
		final Set<Landmark> landmarks=Landmark.getAll(st.getInstance());
		for (final Landmark landmark: landmarks) {
			list.add(landmark.getName());
		}
		list.setValue(selected);
		return list;
	}
}
