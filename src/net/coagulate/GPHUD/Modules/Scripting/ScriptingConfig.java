package net.coagulate.GPHUD.Modules.Scripting;

import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Data.Script;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.CheckBox;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.Paragraph;
import net.coagulate.GPHUD.Interfaces.Outputs.TextHeader;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;

public class ScriptingConfig {
	// ---------- STATICS ----------
	@URL.URLs(url="/configuration/scripting", requiresPermission="Scripting.*")
	public static void configPage(@Nonnull final State st,final SafeMap values) {
		final Form f=st.form();
		f.add(new TextHeader("Scripting Module"));
		f.add(new Paragraph("List of scripts"));
		f.add(Script.getTable(st,st.hasPermission("scripting.delete")));
		f.noForm();
		f.add(new Form(st,true,"/GPHUD/configuration/scripting/create","Create new script"));
	}
	
	@URL.URLs(url="/configuration/scripting/delete", requiresPermission="Scripting.Create")
	public static void deleteScript(@Nonnull final State st,@Nonnull final SafeMap values) {
		final Form f=st.form();
		final String scriptName=values.get("scriptname");
		if (!(scriptName.isEmpty()||values.get("confirm").isEmpty())) {
			Script.find(st,scriptName).delete();
			Audit.audit(true,
			            st,
			            Audit.OPERATOR.AVATAR,
			            null,
			            null,
			            "Delete",
			            values.get("scriptname"),
			            "",
			            "",
			            "Deleted script");
			throw new RedirectionException("/GPHUD/configuration/scripting");
		}
		f.add(new TextHeader("Delete script"));
		f.add("Name of script:").add(new TextInput("scriptname")).br();
		f.add(new CheckBox("confirm")).add("Confirm deletion?  This action can not be undone.").br().br();
		f.add(new Button("Delete"));
	}
	
	@URL.URLs(url="/configuration/scripting/create", requiresPermission="Scripting.Create")
	public static void createScript(@Nonnull final State st,@Nonnull final SafeMap values) {
		final Form f=st.form();
		if (!values.get("scriptname").isEmpty()) {
			Script.create(st,values.get("scriptname"));
			Audit.audit(true,
			            st,
			            Audit.OPERATOR.AVATAR,
			            null,
			            null,
			            "Create",
			            values.get("scriptname"),
			            "",
			            "",
			            "Created script");
			throw new RedirectionException("/GPHUD/configuration/scripting");
		}
		f.add(new TextHeader("Create new script"));
		f.add("Name of script:").add(new TextInput("scriptname")).br();
		f.add(new Button("Create"));
	}
	
	
}
