package net.coagulate.GPHUD.Modules.Menus;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Menus;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import java.util.Map;

/**
 * Configures menus for dialog and hud web panel.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class MenuConfig {

	@URLs(url = "/configuration/menus")
	public static void configure(State st, SafeMap values) {
		Form f = st.form;
		f.add(new TextSubHeader("Dialog menu configuration"));
		Map<String, Integer> menus = Menus.getMenusMap(st);
		for (Map.Entry<String, Integer> entry : menus.entrySet()) {
			f.add("<a href=\"./menus/view/" + entry.getValue() + "\">" + entry.getKey() + "</a><br>");
		}
		if (st.hasPermission("Menus.Config")) {
			f.add("<br><a href=\"./menus/create\">Create new menu</a><br>");
		}
	}

	@URLs(url = "/configuration/menus/view/*")
	public static void viewMenus(State st, SafeMap values) throws SystemException, UserException {
		String[] split = st.getDebasedURL().split("/");
		String id = split[split.length - 1];
		Menus m = Menus.get(Integer.parseInt(id));
		viewMenus(st, values, m);
	}

	public static void viewMenus(State st, SafeMap values, Menus m) throws SystemException, UserException {
		if (m.getInstance() != st.getInstance()) {
			throw new UserException("That menu belongs to a different instance");
		}
		if (st.hasPermission("Menus.Config") && "Submit".equals(values.get("Submit"))) {
			JSONObject json = new JSONObject();
			for (int i = 1; i <= 12; i++) {
				String button = values.get("button" + i);
				String command = values.get("command" + i);
				if (!button.isEmpty() && !command.isEmpty()) {
					json.put("button" + i, button);
					json.put("command" + i, command);
				}
			}
			m.setJSON(json);
			if ("Main".equalsIgnoreCase(m.getName())) {
				JSONObject broadcastupdate = new JSONObject();
				broadcastupdate.put("incommand", "broadcast");
				JSONObject legacymenu = Modules.getJSONTemplate(st, "menus.main");
				broadcastupdate.put("legacymenu", legacymenu.toString());
				st.getInstance().sendServers(broadcastupdate);
			}
		}
		Form f = st.form;
		f.add(new TextHeader("Menu '" + m.getName() + "'"));
		f.add(new Paragraph("Select buttons and relevant commands for the HUD, note you can select another menu as a command.  Commands the user does not have permission to access will be omitted from the menu.  Layout of buttons is as follows:"));
		f.add(new Paragraph("Buttons <B>MUST</B> have labels shorter than 24 characters, and likely only the first twelve or so will fit on the users screen."));
		Table example = new Table();
		f.add(example);
		example.openRow().add("10").add("11").add("12");
		example.openRow().add("7").add("8").add("9");
		example.openRow().add("4").add("5").add("6");
		example.openRow().add("1").add("2").add("3");
		Table t = new Table();
		f.add(new TextSubHeader("Button configuration"));
		f.add(t);
		JSONObject j = m.getJSON();
		for (int i = 1; i <= 12; i++) {
			t.openRow().add("Button " + i);
			t.add(new TextInput("button" + i, j.optString("button" + i, "")));
			t.openRow().add("");
			DropDownList command = DropDownList.getCommandsList(st, "command" + i);
			command.setValue(j.optString("command" + i, ""));
			t.add(command);

		}
		if (st.hasPermission("Menus.Config")) { f.add(new Button("Submit")); }

	}

	@URLs(url = "/configuration/menus/create", requiresPermission = "Menus.Config")
	public static void createMenu(State st, SafeMap values) {
		if ("Submit".equals(values.get("Submit")) && !values.get("name").isEmpty()) {
			Menus menu = Menus.create(st, values.get("name"), values.get("description"), new JSONObject());
			throw new RedirectionException("./view/" + menu.getId());
		}
		Form f = st.form;
		f.add(new TextSubHeader("Create new Dialog Menu"));
		Table t = new Table();
		f.add(t);
		t.openRow().add("Name").add(new TextInput("name"));
		t.openRow().add("Description").add(new TextInput("description"));
		t.openRow().add(new Cell(new Button("Submit"), 2));
	}

}
