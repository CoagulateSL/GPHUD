package net.coagulate.GPHUD.Modules.Menus;

import net.coagulate.Core.Exceptions.User.UserConfigurationException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.Data.Menu;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Templated command implementation, aka a menu command.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class MenuCommand extends Command {

	@Nonnull
	final Command targetcommand;
	final String description;
	final String name;
	final Menu menu;

	public MenuCommand(final State st,
	                   final String name,
					   final Menu menu) {
		this.name = name;
		description="This spawns the "+name+" menu";
		this.menu=menu;
		targetcommand=this;
	}

	// ---------- INSTANCE ----------
	@Override
	public boolean isGenerated() {
		return true;
	}

	@Nonnull
	@Override
	public String description() { return description; }

	@Nonnull
	@Override
	public String notes() { return ""; }

	@Nonnull
	@Override
	public String requiresPermission() { return ""; }

	@Nonnull
	@Override
	public Context context() { return Context.CHARACTER; }

	@Override
	public boolean permitHUD() { return true; }

	@Override
	public boolean permitObject() { return false; }

	@Override
	public boolean permitConsole() { return false; }

	@Override
	public boolean permitWeb() { return false; }

	@Override
	public boolean permitScripting() { return false; }

	@Override
	public boolean permitExternal() { return false; }

	@Nonnull
	@Override
	public List<Argument> getArguments() {
		final List<Argument> args=new ArrayList<>();
		args.add(new MenuArgument(this,menu));
		return args;
	}

	@Override
	public int getArgumentCount() { return 1; }

	@Nonnull
	@Override
	public String getFullName() { return "Menus."+getName(); }

	@Nonnull
	public String getName() { return name; }

	@Nonnull
    @Override
    public Response execute(@Nonnull final State state,
                            @Nonnull final Map<String, Object> arguments) {
		JSONObject definition=menu.getJSON();
        final String selected = (String) arguments.get("choice");
        int choice = -1;
        for (int i = 1; i <= MenuModule.MAX_BUTTONS; i++) {
            if (definition.optString("button" + i, "").equals(selected)) {
                choice = i;
            }
        }
        if (choice == -1) {
            throw new UserInputLookupFailureException("Menu " + getName() + " has no element " + selected, true);
        }
        final String commandtoinvoke = definition.optString("command" + choice, "");
        if (commandtoinvoke.isEmpty()) {
            throw new UserConfigurationException("Menu " + getName() + " command " + selected + " is choice " + choice + " and does not have a command to invoke");
        }
        return Modules.getJSONTemplateResponse(state, commandtoinvoke);
    }
}
