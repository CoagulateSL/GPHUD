package net.coagulate.GPHUD.Modules.Menus;

import net.coagulate.Core.Exceptions.System.SystemConsistencyException;
import net.coagulate.Core.Exceptions.User.UserInputStateException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Data.Menu;
import net.coagulate.GPHUD.Interfaces.Inputs.Button;
import net.coagulate.GPHUD.Interfaces.Inputs.DropDownList;
import net.coagulate.GPHUD.Interfaces.Inputs.TextInput;
import net.coagulate.GPHUD.Interfaces.Outputs.*;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.Interfaces.User.Form;
import net.coagulate.GPHUD.Modules.URL.URLs;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Configures menus for dialog and hud web panel.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class MenuConfig {

	// ---------- STATICS ----------
	@URLs(url="/configuration/menus",
		  requiresPermission = "Menus.*")
	public static void configure(@Nonnull final State st,
	                             final SafeMap values) {
		final boolean candelete=st.hasPermission("Menus.Delete");
		final Form f=st.form();
		f.noForm();
		f.add(new TextSubHeader("Dialog menu configuration"));
		if (candelete && values.containsKey("deletemenu")) {
			try {
				final Menu menu=Menu.get(Integer.parseInt(values.get("deletemenu")));
				if (menu.getInstance()!=st.getInstance()) { throw new SystemConsistencyException("Menu and deleter are from different instances"); }
				final String namewas=menu.getName();
				final int id=menu.getId();
				menu.delete(st);
				f.add(new TextOK("Menu "+namewas+"#"+id+" was deleted!"));
			}
			catch (final UserException e) {
				f.add(new TextError("Failed to delete menu "+values.get("deletemenu")+": "+e.getLocalizedMessage()));
			}
			f.add("<br><br>");
		}
		final Map<String,Integer> menus=Menu.getMenusMap(st);
		for (final Map.Entry<String,Integer> entry: menus.entrySet()) {
			if (candelete) {
				String innercontent="";
				innercontent+="<button "+(entry.getKey()
				                               .equalsIgnoreCase("Main")?"disabled":"")+" style=\"border: 0;\" onclick=\"document.getElementById('delete-"+entry.getValue()+"').style.display='inline';\">Delete</button>";
				innercontent+="<div id=\"delete-"+entry.getValue()+"\" style=\"display: none;\">";
				innercontent+="&nbsp;&nbsp;&nbsp;";
				innercontent+="CONFIRM DELETE? ";
				innercontent+="<form style=\"display: inline;\" method=post>";
				innercontent+="<input type=hidden name=deletemenu value=\""+entry.getValue()+"\">";
				innercontent+="<button style=\"border:0; background-color: #ffc0c0;\" type=submit>Yes, Delete!</button>";
				innercontent+="</form>";
				innercontent+="</div>";
				innercontent+="&nbsp;&nbsp;&nbsp;";
				f.add(innercontent);
			}
			f.add("<a href=\"./menus/view/"+entry.getValue()+"\">"+entry.getKey()+"</a><br>");
		}
		if (st.hasPermission("Menus.Config")) {
			f.add("<br><a href=\"./menus/create\">Create new menu</a><br>");
		}
	}

	@URLs(url="/configuration/menus/view/*")
	public static void viewMenus(@Nonnull final State st,
	                             @Nonnull final SafeMap values) {
		final String[] split=st.getDebasedURL().split("/");
		final String id=split[split.length-1];
		final Menu m=Menu.get(Integer.parseInt(id));
		viewMenus(st,values,m);
	}

	public static void viewMenus(@Nonnull final State st,
	                             @Nonnull final SafeMap values,
	                             @Nonnull final Menu m) {
		if (m.getInstance()!=st.getInstance()) {
			throw new UserInputStateException("That menu belongs to a different instance");
		}
		if (st.hasPermission("Menus.Config") && "Submit".equals(values.get("Submit"))) {
			final JSONObject json=new JSONObject();
			for (int i = 1; i<=MenuModule.MAX_BUTTONS; i++) {
				final String button=values.get("button"+i);
				final String command=values.get("command"+i);
				if (!button.isEmpty() && !command.isEmpty()) {
					json.put("button"+i,button);
					json.put("command"+i,command);
					if (!values.get("permission"+i).isEmpty()) { json.put("permission"+i,values.get("permission"+i)); }
					if (!values.get("permissiongroup"+i).isEmpty()) { json.put("permissiongroup"+i,values.get("permissiongroup"+i)); }
					if (!values.get("charactergroup"+i).isEmpty()) { json.put("charactergroup"+i,values.get("charactergroup"+i)); }
				}
			}
			m.setJSON(json);
			if ("Main".equalsIgnoreCase(m.getName())) {
				/*final JSONObject broadcastupdate=new JSONObject();
				broadcastupdate.put("incommand","broadcast");
				final JSONObject legacymenu=Modules.getJSONTemplate(st,"menus.main");
				broadcastupdate.put("legacymenu",legacymenu.toString());
				st.getInstance().sendServers(broadcastupdate);*/
				st.getInstance().pushConveyances(); // this works now for menus as a conveyance?
			}
		}
		final Form f=st.form();
		f.add(new TextHeader("Menu '"+m.getName()+"'"));
		f.add(new Paragraph(
				"Select buttons and relevant commands for the HUD, note you can select another menu as a command.  Commands the user does not have permission to access will "+"be omitted from the menu.  Layout of buttons is as follows:"));
		f.add(new Paragraph(
				"Buttons <B>MUST</B> have labels shorter than 24 characters, and likely only the first twelve or so will fit on the users screen (if UIX is not enabled in GPHUDClient)."));
		final Table example=new Table();
		f.add(example);
		example.openRow().add("10").add("11").add("12");
		example.openRow().add("7").add("8").add("9");
		example.openRow().add("4").add("5").add("6");
		example.openRow().add("1").add("2").add("3");
		final Table t=new Table();
		f.add(new TextSubHeader("Button configuration"));
		if (st.hasPermission("Menus.Config")) { f.add(new Button("Submit")); }
		f.add(t);
		final JSONObject j=m.getJSON();
		for (int i = 1; i<=MenuModule.MAX_BUTTONS; i++) {
			t.openRow().add("Button "+i);
			final Table tt=new Table();
			t.add(tt);
			tt.add(new TextInput("button"+i,j.optString("button"+i,"")));
			tt.add("Optionally Requires - ");
			tt.add("Permission");
			final DropDownList permissionslist=DropDownList.getPermissionsList(st,"permission"+i);
			permissionslist.setValue(j.optString("permission"+i,""));
			tt.add(permissionslist);
			tt.add("-or- PermissionGroup");
			final DropDownList permissionsgroups=DropDownList.getPermissionsGroups(st,"permissiongroup"+i);
			permissionsgroups.setValue(j.optString("permissiongroup"+i,""));
			tt.add(permissionsgroups);
			tt.add("-or- CharacterGroup");
			final DropDownList charactergroups=DropDownList.getCharacterGroups(st,"charactergroup"+i);
			charactergroups.setValue(j.optString("charactergroup"+i,""));
			tt.add(charactergroups);
			t.openRow().add("");
			final DropDownList command=DropDownList.getCommandsList(st,"command"+i);
			command.setValue(j.optString("command"+i,""));
			t.add(new Cell(command,99));

		}

	}

	@URLs(url="/configuration/menus/create",
	      requiresPermission="Menus.Config")
	public static void createMenu(@Nonnull final State st,
	                              @Nonnull final SafeMap values) {
		if ("Submit".equals(values.get("Submit")) && !values.get("name").isEmpty()) {
			final Menu menu=Menu.create(st,values.get("name"),values.get("description"),new JSONObject());
			throw new RedirectionException("./view/"+menu.getId());
		}
		final Form f=st.form();
		f.add(new TextSubHeader("Create new Dialog Menu"));
		final Table t=new Table();
		f.add(t);
		t.openRow().add("Name").add(new TextInput("name"));
		t.openRow().add("Description").add(new TextInput("description"));
		t.openRow().add(new Cell(new Button("Submit"),2));
	}

}
