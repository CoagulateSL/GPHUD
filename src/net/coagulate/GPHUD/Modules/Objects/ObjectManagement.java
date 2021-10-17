package net.coagulate.GPHUD.Modules.Objects;

import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Obj;
import net.coagulate.GPHUD.Data.ObjType;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Interfaces.Responses.ErrorResponse;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Objects.ObjectTypes.ObjectType;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class ObjectManagement {

	// ---------- STATICS ----------
	@URL.URLs(url="/configuration/objects",
	          requiresPermission="Objects.View")
	public static void index(@Nonnull final State st,
	                         @Nonnull final SafeMap map) {
		final Form f=st.form();
		f.add(new TextHeader("Object Management"));
		f.add(new TextSubHeader("Connected Objects"));
		if (map.containsKey("reboot") && st.hasPermission("Objects.RebootObjects")) {
			final String uuid=map.get("reboot");
			final Obj obj=Obj.findOrNull(st,uuid);
			if (obj!=null) {
				obj.validate(st);
				final JSONObject reboot=new JSONObject();
				reboot.put("reboot","Rebooted via web site by "+st.getAvatarNullable());
				new Transmission(obj,reboot).start();
				f.add("<p><i>Rebooted object "+obj.getName()+" "+uuid+"</i></p>");
				Audit.audit(true,st,Audit.OPERATOR.AVATAR,null,null,"Reboot",obj.getName(),"","","Rebooted object "+uuid);
			}
		}
		if (map.containsKey("reallyshutdown") && st.hasPermission("Objects.ShutdownObjects")) {
			final String uuid=map.get("reallyshutdown");
			final Obj obj=Obj.findOrNull(st,uuid);
			if (obj!=null) {
				obj.validate(st);
				final JSONObject shutdown=new JSONObject();
				shutdown.put("shutdown","Shutdown via web site by "+st.getAvatarNullable());
				new Transmission(obj,shutdown).start();
				f.add("<p><i>Shutdown object "+obj.getName()+" "+uuid+"</i></p>");
				Audit.audit(true,st,Audit.OPERATOR.AVATAR,null,null,"Shutdown",obj.getName(),"","","Shutdown object "+uuid);
			}
		}
		f.add(Obj.dumpObjects(st));
		f.add(new TextSubHeader("Assignable Behaviours"));
		f.add(ObjType.dumpTypes(st));
		f.add("<br/><a href=\"/GPHUD/configuration/objects/createtype\">Create new object type</a>");
	}

	@URL.URLs(url="/configuration/objects/createtype",
	          requiresPermission="Objects.ObjectTypes")
	public static void createObjectType(@Nonnull final State st,
	                                    @Nonnull final SafeMap map) {
		if (map.get("Create").equalsIgnoreCase("Create") && ObjectType.getObjectTypesSet().contains(map.get("behaviour"))) {
			final JSONObject jsonbase=new JSONObject();
			jsonbase.put("behaviour",map.get("behaviour"));
			final ObjType ot=ObjType.create(st,map.get("name"),jsonbase);
			Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Create","ObjectType",null,map.get("name"),"Created new object type of behaviour "+map.get("behaviour"));
			throw new RedirectionException("/GPHUD/configuration/objects/objecttypes/"+ot.getId());
		}
		final Form f=st.form();
		final Table input=new Table();
		f.add(input);
		input.add("Name").add(new TextInput("name"));
		final DropDownList behaviours=ObjectType.getDropDownList(st);
		input.openRow().add("Behaviour").add(behaviours);
		input.openRow().add(new Cell(new Button("Create"),2));
	}

	@URL.URLs(url="/configuration/objects/objecttypes/*",
	          requiresPermission="Objects.ObjectTypes")
	public static void editObjectType(@Nonnull final State st,
	                                  final SafeMap map) {
		st.postMap(map);
		final String[] parts=st.getDebasedNoQueryURL().split("/");
		if (parts.length<5) { throw new UserInputValidationParseException("URI misformed, no ID found"); }
		final ObjType t=ObjType.get(Integer.parseInt(parts[4]));
		t.validate(st);
		final Form f=st.form();
		f.add(new TextHeader("Object Type: "+t.getName()));
		final ObjectType ot=ObjectType.materialise(st,t);
		ot.update(st);
		f.add(ot.explainHtml());
		ot.editForm(st);
	}

	@Nonnull
	@Command.Commands(description="Gets the Object Driver Script",
	                  permitScripting=false,
	                  permitUserWeb=false,
	                  context=Command.Context.AVATAR,
	                  requiresPermission="Objects"+".GetDriver",
	                  permitExternal=false,
	                  permitObject=false)
	public static Response getDriver(@Nonnull final State st) {
		final JSONObject json=new JSONObject();
		json.put("incommand","servergive");
		json.put("itemname","GPHUD Object Driver");
		json.put("giveto",st.getAvatar().getUUID());
		st.getRegion().sendServer(json);
		return new OKResponse("OK - Sent you an Object Driver script");
	}

	@Nonnull
	@Command.Commands(description="Remove an object type by name",
					  context=Command.Context.AVATAR,
					  requiresPermission="Objects.ObjectTypes",
					  permitExternal=false,
					  permitObject=false,
					  permitScripting=false)
	public static Response deleteObjectType(@Nonnull final State st,
										  @Argument.Arguments(name="name", description="Object Type to remove",
															  type=Argument.ArgumentType.TEXT_ONELINE,
															  max=64) final String name) {
		final ObjType ot=ObjType.get(st,name);
		if (ot==null) { return new ErrorResponse("Can not delete object type "+name+" - it does not exist"); }
		ot.delete();
		Audit.audit(st,Audit.OPERATOR.AVATAR,null,null,"Delete",name,"","","Deleted Object Type "+name);
		return new OKResponse("Deleted ObjectType "+name);
	}

	@URL.URLs(url="/configuration/objects/deleteobjecttype",
			  requiresPermission="Objects.ObjectTypes")
	public static void toggleTemplatable(@Nonnull final State st,
										 @Nonnull final SafeMap values) {
		Modules.simpleHtml(st,"Objects.DeleteObjectType",values);
	}


}
