package net.coagulate.GPHUD.Modules.Menus;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
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

	@Nonnull
	@Override
	public Context context() { return Context.CHARACTER; }

	@Nonnull
	@Override
	public String description() { return description; }

	@Nonnull
	@Override
	public List<String> getArgumentNames(final State st) {
		final List<String> args=new ArrayList<>();
		args.add("choice");
		return args;
	}

	@Override
	public int getArgumentCount() { return 1; }

	@Nonnull
	@Override
	public List<Argument> getArguments() {
		final List<Argument> args=new ArrayList<>();
		args.add(new MenuArgument(this,definition));
		return args;
	}

	@Nonnull
	@Override
	public String getFullName() { return "Menus."+getName(); }

	@Nonnull
	public String getName() { return name; }

	@Override
	public boolean permitConsole() { return false; }

	@Override
	public boolean permitJSON() { return true; }

	@Override
	public boolean permitUserWeb() { return false; }

	@Override
	public boolean permitScripting() { return false; }

	@Override
	public boolean permitObject() { return true; }

	@Nonnull
	@Override
	public String getFullMethodName() { return getClass()+".run()"; }

	@Nonnull
	@Override
	public String requiresPermission() { return ""; }

	@Nonnull
	@Override
	public Response run(@Nonnull final State st,
	                    @Nonnull final SafeMap parametermap) {
		final String selected=parametermap.get("choice");
		int choice=0;
		for (int i=1;i<=12;i++) { if (definition.optString("button"+i,"").equals(selected)) { choice=i; } }
		return Modules.getJSONTemplateResponse(st,definition.getString("command"+choice));
	}

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

	@Nonnull
	@Override
	public List<Argument> getInvokingArguments() {
		return getArguments();
	}

	@Override
	public int getInvokingArgumentCount() {
		return getArgumentCount();
	}

	@Override
	public boolean isGenerated() {
		return true;
	}
}
