package net.coagulate.GPHUD.Modules.Alias;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.Exceptions.User.UserConfigurationException;
import net.coagulate.Core.Exceptions.User.UserConfigurationRecursionException;
import net.coagulate.Core.Exceptions.User.UserInputLookupFailureException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Templater;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Templated command implementation, aka an alias command.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class AliasCommand extends Command {

	final JSONObject definition;
	@Nullable
	Command targetcommand;
	final String name;
	@Nonnull
	String fail="";

	public AliasCommand(@Nonnull final State st,
	                    final String name,
	                    final JSONObject newdef) {
		super();
		definition=newdef;
		this.name=name;
		if (st.hasModule(definition.getString("invoke"))) {
			try { targetcommand=Modules.getCommandNullable(st,definition.getString("invoke")); }
			catch (UserConfigurationRecursionException e) {
				targetcommand=null;
				fail=e.getLocalizedMessage();
			}
		}
		else {
			targetcommand=null;
			fail="Module "+Modules.extractModule(definition.getString("invoke"))+" is not enabled.";
		}
	}

	// ---------- INSTANCE ----------
	public JSONObject getDefinition() { return definition; }

	@Nonnull
	public Command getTargetCommand() {
		if (targetcommand==null) {
			throw new SystemBadValueException("Attempt to access null target command in Alias Command "+getName());
		}
		return targetcommand;
	}

	@Override
	public boolean isGenerated() {
		return true;
	}

	@Override
	public String description() {
		if (targetcommand==null) { return "The target command is not reachable, "+fail; }
		return targetcommand.description();
	}

	@Nonnull
	@Override
	public String notes() {
		if (targetcommand==null) { return ""; }
		return targetcommand.notes();
	}

	@Override
	public String requiresPermission() {
		if (targetcommand==null) { return ""; }
		return targetcommand.requiresPermission();
	}

	@Override
	public Context context() {
		if (targetcommand==null) { return Context.ANY; }
		return targetcommand.context();
	}

	@Override
	public boolean permitHUD() {
		if (targetcommand==null) { return false; }
		return targetcommand.permitHUD();
	}

	@Override
	public boolean permitObject() {
		if (targetcommand==null) { return false; }
		return targetcommand.permitObject();
	}

	@Override
	public boolean permitConsole() {
		if (targetcommand==null) { return false; }
		return targetcommand.permitConsole();
	}

	@Override
	public boolean permitWeb() {
		if (targetcommand==null) { return false; }
		return targetcommand.permitWeb();
	}

	@Override
	public boolean permitScripting() {
		if (targetcommand==null) { return false; }
		return targetcommand.permitScripting();
	}

	@Override
	public boolean permitExternal() {
		if (targetcommand==null) { return false; }
		return targetcommand.permitExternal();
	}

	@Nonnull
	@Override
	public List<Argument> getArguments() {
		if (targetcommand==null) { return new ArrayList<>(); }
		final List<Argument> args=targetcommand.getArguments();
		final List<Argument> remainingargs=new ArrayList<>();
		for (final Argument a: args) {
			if (!definition.has(a.name())) { remainingargs.add(a); }
			if (definition.has(a.name()+"-desc") && !definition.optString(a.name()+"-desc","").isEmpty()) {
				a.overrideDescription(definition.getString(a.name()+"-desc"));
			}
		}
		return remainingargs;
	}

	@Override
	public int getArgumentCount() { return getArguments().size(); }

	@Nonnull
	@Override
	public String getFullName() { return "Alias."+getName(); }

	@Nonnull
	public String getName() { return name; }

	// ----- Internal Instance -----
	@Override
	protected Response execute(final State state,
	                           final Map<String,Object> arguments) {
		if (targetcommand==null) {
			throw new UserConfigurationException("Error: Alias targets command "+name+", "+fail);
		}
		/*
		if (targetcommand.getFullName().toLowerCase().startsWith("gphudclient.quickbutton")) {
			throw new UserConfigurationException("It is not permitted to call quickbuttons from aliases (in alias "+getName()+")");
		}*/
		// assume target.  this sucks :P
		if (getDefinition().has("target")) {
			arguments.put("target",getDefinition().getString("target"));
		}

		if (arguments.containsKey("target")) {
			final Object vobject=arguments.get("target");
			if (vobject instanceof String) {
				String v=(String) vobject;
				final Char targchar;
				if (v.startsWith(">")) {
					v=v.substring(1);
					try {
						final User a=User.findUsername(v,false);
						targchar=Char.getActive(a,state.getInstance());
					}
					catch (@Nonnull final NoDataException e) {
						throw new UserInputLookupFailureException("Unable to find character or avatar named '"+v+"'");
					}
				}
				else {
					targchar=Char.resolve(state,v);
				}
				if (targchar!=null) { state.setTarget(targchar); }
			}
			if (vobject instanceof Char) {
				state.setTarget((Char) vobject);
			}
		}

		for (final String key: getDefinition().keySet()) {
			if (!"invoke".equalsIgnoreCase(key)) {
				boolean numeric=false;
				boolean integer=false;
				boolean delaytemplating=false;
				for (final Argument arg: getTargetCommand().getArguments()) {
					if (arg.name().equals(key)) {
						if (arg.type()==ArgumentType.FLOAT || arg.type()==ArgumentType.INTEGER) {
							numeric=true;
							if (arg.type()==ArgumentType.INTEGER) { integer=true; }
						}
						if (arg.delayTemplating()) { delaytemplating=true; }
						if (!delaytemplating) {
							arguments.put(key,convertArgument(state,arg,Templater.template(state,getDefinition().getString(key),numeric,integer)));
						}
						else {
							arguments.put(key,convertArgument(state,arg,getDefinition().getString(key)));
						}
					}
				}
			}
		}
		return getTargetCommand().run(state,arguments);
	}

}
