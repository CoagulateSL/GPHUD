package net.coagulate.GPHUD.Modules.Objects;

import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.ObjectTypes;
import net.coagulate.GPHUD.Data.Objects;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.Cell;
import net.coagulate.GPHUD.Interfaces.Outputs.Table;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.Outputs.TextSubHeader;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Interfaces.Responses.OKResponse;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Interfaces.System.Transmission;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Objects.ObjectTypes.ObjectType;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;

public class ObjectManagement {

	@URL.URLs(url = "/configuration/objects",requiresPermission = "Objects.View")
	public static void index(@Nonnull final State st, @Nonnull final SafeMap map) {
		final Form f= st.form();
		f.add(new TextHeader("Object Management"));
		f.add(new TextSubHeader("Connected Objects"));
		if (map.containsKey("reboot") && st.hasPermission("Objects.RebootObjects")) {
			final String uuid=map.get("reboot");
			final Objects obj = Objects.findOrNull(st, uuid);
			if (obj!=null) {
				obj.validate(st);
				final JSONObject reboot=new JSONObject();
				reboot.put("reboot","Rebooted via web site by "+st.getAvatarNullable());
				new Transmission(obj,reboot).start();
				f.add("<p><i>Rebooted object "+obj.getName()+" "+uuid+"</i></p>");
				Audit.audit(true,st, Audit.OPERATOR.AVATAR,null,null,"Reboot",obj.getName(),"","","Rebooted object "+uuid);
			}
		}
		if (map.containsKey("reallyshutdown") && st.hasPermission("Objects.ShutdownObjects")) {
			final String uuid=map.get("reallyshutdown");
			final Objects obj = Objects.findOrNull(st, uuid);
			if (obj!=null) {
				obj.validate(st);
				final JSONObject shutdown=new JSONObject();
				shutdown.put("shutdown","Shutdown via web site by "+st.getAvatarNullable());
				new Transmission(obj,shutdown).start();
				f.add("<p><i>Shutdown object "+obj.getName()+" "+uuid+"</i></p>");
				Audit.audit(true,st, Audit.OPERATOR.AVATAR,null,null,"Shutdown",obj.getName(),"","","Shutdown object "+uuid);
			}
		}
		f.add(Objects.dumpObjects(st));
		f.add(new TextSubHeader("Assignable Behaviours"));
		f.add(ObjectTypes.dumpTypes(st));
		f.add("<br/><a href=\"/GPHUD/configuration/objects/createtype\">Create new object type</a>");
	}

	@URL.URLs(url="/configuration/objects/createtype",requiresPermission="Objects.ObjectTypes")
	public static void createObjectType(@Nonnull final State st, @Nonnull final SafeMap map) {
		if (map.get("Create").equalsIgnoreCase("Create")) {
			final JSONObject jsonbase=new JSONObject();
			jsonbase.put("behaviour",map.get("behaviour"));
			final ObjectTypes ot=ObjectTypes.create(st,map.get("name"),jsonbase);
			Audit.audit(st, Audit.OPERATOR.AVATAR,null,null,"Create","ObjectType",null,map.get("name"),"Created new object type of behaviour "+map.get("behaviour"));
			throw new RedirectionException("/GPHUD/configuration/objects/objecttypes/"+ot.getId());
		}
		final Form f= st.form();
		final Table input=new Table(); f.add(input);
		input.add("Name").add(new TextInput("name"));
		final DropDownList behaviours= ObjectType.getDropDownList(st);
		input.openRow().add("Behaviour").add(behaviours);
		input.openRow().add(new Cell(new Button("Create"),2));
	}

	@URL.URLs(url="/configuration/objects/objecttypes/*",requiresPermission="Objects.ObjectTypes")
	public static void editObjectType(@Nonnull final State st, final SafeMap map) {
		st.postmap=map;
		final String[] parts=st.getDebasedNoQueryURL().split("/");
		if (parts.length<5) { throw new UserInputValidationParseException("URI misformed, no ID found"); }
		final ObjectTypes t=ObjectTypes.get(Integer.parseInt(parts[4]));
		t.validate(st);
		final Form f= st.form();
		f.add(new TextHeader("Object Type: "+t.getName()));
		final ObjectType ot=ObjectType.materialise(st,t);
		ot.update(st);
		f.add(ot.explainHtml());
		ot.editForm(st);
	}

	@Nonnull
	@Command.Commands(description = "Gets the Object Driver Script",permitScripting = false,permitUserWeb = false,context = Command.Context.AVATAR,requiresPermission = "Objects.GetDriver")
	public static Response getDriver(@Nonnull final State st) {
		final JSONObject json = new JSONObject();
		json.put("incommand", "servergive");
		json.put("itemname", "GPHUD Object Driver");
		json.put("giveto", st.getAvatarNullable().getUUID());
		st.getRegion().sendServer(json);
		return new OKResponse("OK - Sent you an Object Driver script");
	}

}
