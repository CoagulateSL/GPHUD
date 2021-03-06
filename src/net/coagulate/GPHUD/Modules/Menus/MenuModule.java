package net.coagulate.GPHUD.Modules.Menus;

import net.coagulate.GPHUD.Data.Menu;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.ModuleAnnotation;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Templater.Template;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.TreeMap;

/**
 * Custom module for aliases, has dynamically generated commands.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class MenuModule extends ModuleAnnotation {

    public static final int MAX_BUTTONS = 40;

    public MenuModule(final String name,
                      final ModuleDefinition annotation) {
        super(name, annotation);
    }

    // ---------- STATICS ----------
    @Nonnull
    @Template(name = "MAINMENU",
              description = "The packaged form of this users main menu")
    public static String generateMainMenu(final State st,
                                          final String key) {
        if (st.mainMenuTemplate == null) {
            JSONObject menu = Modules.getJSONTemplate(st, "menus.main");
            if (st.getCharacter().getProtocol() >= 4) { // shortened format
                int button = 0;
                StringBuilder newMainMenuTemplate = new StringBuilder();

                boolean first = true;
                while (menu.has("arg0button" + button)) {
                    if (!first) {
                        newMainMenuTemplate.append("|");
                    } else {
                        first = false;
                    }
                    newMainMenuTemplate.append(menu.getString("arg0button" + button));
                    button++;
                }
                st.mainMenuTemplate = newMainMenuTemplate.toString();
            } else {
                st.mainMenuTemplate = menu.toString();
            }
        }
        return st.mainMenuTemplate;
    }

    @Override
    public boolean isGenerated() {
        return false;
    }

    @Nonnull
    @Override
    public Command getCommandNullable(@Nonnull final State st,
                                      @Nonnull final String commandName) {
        return new MenuCommand(st, commandName, Menu.getMenu(st, commandName).getJSON());
    }

    @Nonnull
    @Override
    public Map<String, Command> getCommands(@Nonnull final State st) {
        final Map<String, Command> commands = new TreeMap<>();
        final Map<String, JSONObject> templates = Menu.getTemplates(st);
        for (final Map.Entry<String, JSONObject> entry : templates.entrySet()) {
            final String name = entry.getKey();
            commands.put(name, new MenuCommand(st, name, entry.getValue()));
        }
        return commands;
    }

    @Override
    protected void initialiseInstance(@Nonnull final State st) {
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
        Menu.create(st, "Main", "Main Menu", j);
        j = new JSONObject();
        j.put("button1", "AwardXP");
        j.put("command1", "Faction.AwardXP");
        j.put("button2", "Invite");
        j.put("command2", "Faction.Invite");
        Menu.create(st, "Faction", "Faction Menu", j);
    }

    // ---------- INSTANCE ----------

}
