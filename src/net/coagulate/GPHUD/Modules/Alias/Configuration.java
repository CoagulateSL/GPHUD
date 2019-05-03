package net.coagulate.GPHUD.Modules.Alias;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Alias;
import net.coagulate.GPHUD.Data.Audit;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import java.util.Map;

/**
 * Configure your aliases here.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Configuration {
	@URLs(url = "/configuration/alias", requiresPermission = "Alias.Config")
	public static void aliasesList(State st, SafeMap values) throws SystemException {
		Form f = st.form;
		f.noForm();
		f.add(new TextSubHeader("Alias Configuration"));

		if (values.containsKey("deletealias") && st.isInstanceOwner()) {
			Alias alias = Alias.getAlias(st, values.get("deletealias"));
			if (alias == null) {
				f.add("<p color=red>Alias '" + values.get("deletealias") + "' was not fount</p>");
			} else {
				alias.delete();
				f.add("<p color=green>Alias '" + values.get("deletealias") + "' has been deleted</p>");
			}
		}

		Map<String, Alias> aliases = Alias.getAliasMap(st);
		int counter = 0;
		for (String name : aliases.keySet()) {
			String innercontent = "";
			if (st.isInstanceOwner()) {
				innercontent += "<button style=\"border: 0;\" onclick=\"document.getElementById('delete-" + counter + "').style.display='inline';\">Delete</button>";
				innercontent += "<div id=\"delete-" + counter + "\" style=\"display: none;\">";
				innercontent += "&nbsp;&nbsp;&nbsp;";
				innercontent += "CONFIRM DELETE? ";
				innercontent += "<form style=\"display: inline;\" method=post>";
				innercontent += "<input type=hidden name=deletealias value=\"" + name + "\">";
				innercontent += "<button style=\"border:0; background-color: #ffc0c0;\" type=submit>Yes, Delete!</button>";
				innercontent += "</form>";
				innercontent += "</div>";
				innercontent += "&nbsp;&nbsp;&nbsp;";
			}
			innercontent += "<a href=\"./alias/view/" + aliases.get(name).getId() + "\">" + name + "</a>";
			innercontent += "<br>";
			f.add(innercontent); // bleugh
			counter++;
		}
		f.add("<br>");
		f.add(new Form(st, false, "./alias/create", "Create"));
	}

	@URLs(url = "/configuration/alias/create", requiresPermission = "Alias.Config")
	public static void createAlias(State st, SafeMap values) throws SystemException, UserException {
		if (values.get("Submit").equals("Submit") && !values.get("name").isEmpty() && !values.get("command").isEmpty()) {
			JSONObject template = new JSONObject();
			template.put("invoke", values.get("command"));
			try {
				Alias newalias = Alias.create(st, values.get("name"), template);
				Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "Create", "Alias", null, values.get("command"), "Avatar created new alias");
				throw new RedirectionException("./view/" + newalias.getId());
			} catch (UserException e) {
				st.form.add(new Paragraph(new TextError("Creation failed : " + e.getMessage())));
			}
		}
		Form f = st.form;
		Table t = new Table();
		f.add(t);
		t.openRow().add("Alias Name").add(new TextInput("name"));
		t.openRow().add("Base Command").add(DropDownList.getCommandsList(st, "command", false));
		t.openRow().add(new Cell(new Button("Submit"), 2));
	}

	@URLs(url = "/configuration/alias/view/*")
	public static void viewAlias(State st, SafeMap values) throws SystemException, UserException {
		String split[] = st.getDebasedURL().split("/");
		String id = split[split.length - 1];
		Alias a = Alias.get(Integer.parseInt(id));
		viewAlias(st, values, a);
	}

	public static void viewAlias(State st, SafeMap values, Alias a) throws SystemException, UserException {
		a.validate(st);
		Form f = st.form;
		if (values.get("Update").equals("Update")) {
			if (st.hasPermissionOrAnnotateForm("Alias.Config")) {
				JSONObject old = a.getTemplate();
				JSONObject template = new JSONObject();
				for (String k : values.keySet()) {
					if (!k.equals("Update")) { template.put(k, values.get(k)); }
				}
				template.put("invoke", old.get("invoke"));
				a.setTemplate(template);
				Audit.audit(st, Audit.OPERATOR.AVATAR, null, null, "Updated", a.getName(), old.toString(), template.toString(), "Avatar updated command alias");
				f.add(new TextOK("Template Updated"));
			}
		}
		Table t = new Table();
		f.add(new TextHeader("Alias Configuration : " + a.getName()));
		JSONObject template = a.getTemplate();
		f.add(new Paragraph("Invokes command " + template.getString("invoke")));
		f.add(new Paragraph(new TextSubHeader("Template")));
		f.add(t);
		t.add(new HeaderRow().add("Argument Name").add("Templated Value").add("Originating Type").add("Originating Description").add("Replaced Description"));
		Command c = Modules.getCommand(st, template.getString("invoke"));
		for (Argument arg : c.getArguments()) {
			if (!template.has(arg.getName())) { template.put(arg.getName(), ""); }
		}

		for (String name : template.keySet()) {
			if (!name.equals("invoke") && !name.endsWith("-desc")) {
				t.openRow().add(name).add(new TextInput(name, template.getString(name)));
				Argument arg = null;
				for (Argument anarg : c.getArguments()) { if (anarg.getName().equals(name)) { arg = anarg; }}
				if (arg != null) {
					t.add(arg.type().toString());
					t.add(arg.description());
					String desc = template.optString(name + "-desc", "");
					t.add(new TextInput(name + "-desc", desc));
					if (arg.delayTemplating()) { t.add("  <i> ( This parameter uses delayed templating ) </i>"); }
				}
			}
		}
		if (st.hasPermission("Alias.Config")) { f.add(new Button("Update", "Update")); }
	}


}
