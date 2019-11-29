package net.coagulate.GPHUD.Modules.Menus;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Menus;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import java.util.Map;
import java.util.TreeMap;

/**
 * Custom module for aliases, has dynamically generated commands.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class MenuModule extends ModuleAnnotation {

	public MenuModule(String name, ModuleDefinition annotation) {
		super(name, annotation);
	}

	@Override
	public Map<String, Command> getCommands(State st) throws UserException, SystemException {
		Map<String, Command> commands = new TreeMap<>();
		Map<String, JSONObject> templates = Menus.getTemplates(st);
		for (Map.Entry<String, JSONObject> entry : templates.entrySet()) {
			String name = entry.getKey();
			commands.put(name, new MenuCommand(st, name, entry.getValue()));
		}
		return commands;
	}

	@Override
	public Command getCommand(State st, String commandname) {
		return new MenuCommand(st, commandname, Menus.getMenu(st, commandname).getJSON());
	}

	@Override
	protected void initialiseInstance(State st) {
		// some useful defaults
		JSONObject j = new JSONObject();
		j.put("button1", "Roll");
		j.put("command1", "Roller.Roll");
		j.put("button2", "Roll Against");
		j.put("command2", "Roller.RollAgainst");
		j.put("button3", "Default Roll");
		j.put("command3", "Alias.Roll");
		j.put("button4", "Open Website");
		j.put("command4", "GPHUDClient.OpenWebsite");
		j.put("button5", "Damage Roll");
		j.put("command5", "Alias.Damage");
		j.put("button6", "Faction");
		j.put("command6", "Menus.Faction");
		Menus.create(st, "Main", "Main Menu", j);
		j = new JSONObject();
		j.put("button1", "AwardXP");
		j.put("command1", "Faction.AwardXP");
		j.put("button2", "Invite");
		j.put("command2", "Faction.Invite");
		Menus.create(st, "Faction", "Faction Menu", j);
	}

	@Override
	public boolean isGenerated() {
		return false;
	}

}
