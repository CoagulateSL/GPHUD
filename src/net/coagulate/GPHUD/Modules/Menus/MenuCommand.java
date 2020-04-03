package net.coagulate.GPHUD.Modules.Menus;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.User.UserConfigurationException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Templated command implementation, aka a menu command.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class MenuCommand extends Command {

	final JSONObject definition;
	@Nonnull
	final Command targetcommand;
	final String description="Pick a menu item item item. :P";
	final String name;

	public MenuCommand(final State st,
	                   final String name,
	                   final JSONObject newdef) {
		super();
		definition=newdef;
		this.name=name;
		targetcommand=this;
	}

	// ---------- INSTANCE ----------
	@Nonnull
	@Override
	public Method getMethod() {
		try {
			return getClass().getDeclaredMethod("run",State.class,SafeMap.class);
		}
		catch (@Nonnull final NoSuchMethodException|SecurityException ex) {
			throw new SystemImplementationException("Issue locating RUN command for MenuCommand, this makes no sense :)");
		}
	}

	@Override
	public boolean isGenerated() {
		return true;
	}

	@Nonnull
	@Override
	public String description() { return description; }

	@Nonnull
	@Override
	public String requiresPermission() { return ""; }

	@Nonnull
	@Override
	public Context context() { return Context.CHARACTER; }

	@Override
	public boolean permitJSON() { return true; }

	@Override
	public boolean permitObject() { return true; }

	@Override
	public boolean permitConsole() { return false; }

	@Override
	public boolean permitUserWeb() { return false; }

	@Override
	public boolean permitScripting() { return false; }

	@Override
	public boolean permitExternal() { return false; }

	@Nonnull
	@Override
	public List<Argument> getArguments() {
		final List<Argument> args=new ArrayList<>();
		args.add(new MenuArgument(this,definition));
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
	public List<String> getArgumentNames(final State st) {
		final List<String> args=new ArrayList<>();
		args.add("choice");
		return args;
	}

	@Nonnull
	@Override
	public List<Argument> getInvokingArguments() {
		return getArguments();
	}

	@Nonnull
	@Override
	public Response run(@Nonnull final State st,
	                    @Nonnull final SafeMap parametermap) {
		final String selected=parametermap.get("choice");
		int choice=-1;
		for (int i=1;i<=12;i++) { if (definition.optString("button"+i,"").equals(selected)) { choice=i; } }
		if (choice==-1) { throw new UserInputLookupFailureException("Menu "+getName()+" has no element "+selected); }
		final String commandtoinvoke=definition.optString("command"+choice,"");
		if (commandtoinvoke.isEmpty()) {
			throw new UserConfigurationException("Menu "+getName()+" command "+selected+" is choice "+choice+" and does not have a command to invoke");
		}
		return Modules.getJSONTemplateResponse(st,commandtoinvoke);
	}

	@Nonnull
	@Override
	public String getFullMethodName() { return getClass()+".run()"; }

	@Override
	public int getInvokingArgumentCount() {
		return getArgumentCount();
	}
}
