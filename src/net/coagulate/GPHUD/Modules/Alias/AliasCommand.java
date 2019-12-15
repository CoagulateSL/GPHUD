package net.coagulate.GPHUD.Modules.Alias;

import net.coagulate.Core.Database.NoDataException;
import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Data.Char;
import net.coagulate.GPHUD.Interfaces.Responses.Response;
import net.coagulate.GPHUD.Modules.Argument;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Command;
import net.coagulate.GPHUD.Modules.Modules;
import net.coagulate.GPHUD.Modules.Templater;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Data.User;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Templated command implementation, aka an alias command.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class AliasCommand extends Command {

	final JSONObject definition;
	@Nullable
	final Command targetcommand;
	final String name;
	@Nonnull
	String fail = "";

	public AliasCommand(@Nonnull State st, String name, JSONObject newdef) throws UserException, SystemException {
		super();
		definition = newdef;
		this.name = name;
		if (st.hasModule(definition.getString("invoke"))) {
			targetcommand = Modules.getCommandNullable(st, definition.getString("invoke"));
		} else {
			targetcommand = null;
			fail = "Module " + Modules.extractModule(definition.getString("invoke")) + " is not enabled.";
		}
	}

	public JSONObject getDefinition() { return definition; }

	@Nonnull
	public Command getTargetCommand() {
		if (targetcommand==null) { throw new SystemException("Attempt to access null target command in Alias Command "+getName()); }
		return targetcommand;
	}

	@Override
	public Context context() {
		if (targetcommand == null) { return Context.ANY; }
		return targetcommand.context();
	}

	@Override
	public String description() {
		if (targetcommand == null) { return "The target command is not reachable, " + fail; }
		return targetcommand.description();
	}


	@Override
	public List<String> getArgumentNames(State st) throws UserException {
		if (targetcommand == null) { return new ArrayList<>(); }
		List<String> args = targetcommand.getArgumentNames(st);
		for (String key : definition.keySet()) {
			args.remove(key);
		}
		return args;
	}

	@Override
	public int getArgumentCount() { return getArguments().size(); }

	@Nonnull
	@Override
	public List<Argument> getArguments() {
		if (targetcommand == null) { return new ArrayList<>(); }
		List<Argument> args = targetcommand.getArguments();
		List<Argument> remainingargs = new ArrayList<>();
		for (Argument a : args) {
			if (!definition.has(a.getName())) { remainingargs.add(a); }
			if (definition.has(a.getName() + "-desc") && !definition.optString(a.getName() + "-desc", "").isEmpty()) {
				a.overrideDescription(definition.getString(a.getName() + "-desc"));
			}
		}
		return remainingargs;
	}

	@Nonnull
	@Override
	public String getFullName() { return "Alias." + getName(); }

	public String getName() { return name; }

	@Override
	public boolean permitConsole() {
		if (targetcommand == null) { return false; }
		return targetcommand.permitConsole();
	}

	@Override
	public boolean permitJSON() {
		if (targetcommand == null) { return false; }
		return targetcommand.permitJSON();
	}

	@Override
	public boolean permitScripting() {
		if (targetcommand == null) { return false; }
		return targetcommand.permitScripting();
	}

	@Override
	public boolean permitObject() {
		if (targetcommand == null) { return false; }
		return targetcommand.permitObject();
	}

	@Override
	public boolean permitUserWeb() {
		if (targetcommand == null) { return false; }
		return targetcommand.permitUserWeb();
	}

	@Nonnull
	@Override
	public String getFullMethodName() {
		if (targetcommand == null) { return "Can not invoke - " + fail; }
		return this.getClass().getCanonicalName() + "(" + getName() + "), will invoke " + targetcommand.getFullMethodName();
	}

	@Override
	public String requiresPermission() {
		if (targetcommand == null) { return ""; }
		return targetcommand.requiresPermission();
	}

	@Override
	public Response run(@Nonnull State st, @Nonnull SafeMap parametermap) throws UserException, SystemException {
		if (targetcommand == null) { throw new UserException("Error: Alias targets command " + name + ", " + fail); }
		// assume target.  this sucks :P
		if (parametermap.containsKey("target")) {
			String v = parametermap.get("target");
			Char targchar = null;
			if (v.startsWith(">")) {
				v = v.substring(1);
				try {
					User a = User.findMandatory(v);
					targchar = Char.getActive(a, st.getInstance());
				} catch (NoDataException e) {
					throw new UserException("Unable to find character or avatar named '" + v + "'");
				}
			} else {
				targchar = Char.resolve(st, v);
			}
			if (targchar != null) { st.setTarget(targchar); }
		}


		Command consider = this;
		while (consider instanceof AliasCommand) {
			AliasCommand ac = (AliasCommand) consider;
			//System.out.println("Processing "+ac.getFullName());
			for (String key : ac.getDefinition().keySet()) {
				if (!"invoke".equalsIgnoreCase(key)) {
					boolean numeric = false;
					boolean integer = false;
					boolean delaytemplating = false;
					for (Argument arg : ac.getTargetCommand().getArguments()) {
						if (arg.getName().equals(key)) {
							if (arg.type() == ArgumentType.FLOAT || arg.type() == ArgumentType.INTEGER) {
								numeric = true;
								if (arg.type() == ArgumentType.INTEGER) { integer = true; }
							}
							if (arg.delayTemplating()) { delaytemplating = true; }
						}
					}
					//System.out.println("Here for key "+key+" with delaytemplating as "+delaytemplating);
					//System.out.println("Existing parameter map is "+parametermap.get(key));
					if (ac.getDefinition().has(key)) {
						//System.out.println("Definition is "+ac.getDefinition().getString(key));
						if (!delaytemplating) {
							parametermap.put(key, Templater.template(st, ac.getDefinition().getString(key), numeric, integer));
						} else {
							parametermap.put(key, ac.getDefinition().getString(key));
						}
					}
					//else { System.out.println("No definition for key "+key); }
				}
			}
			consider = ac.getTargetCommand();
		}
		//parametermap.debugDump();
		return super.run(st, parametermap);
	}

	@Nonnull
	@Override
	public Method getMethod() {
		if (targetcommand == null) { throw new SystemException("Unable to getMethod on null targetcommand"); }
		return targetcommand.getMethod();
	}

	@Override
	public List<Argument> getInvokingArguments() {
		if (targetcommand == null) { return new ArrayList<>(); }
		return targetcommand.getInvokingArguments();
	}

	@Override
	public int getInvokingArgumentCount() {
		if (targetcommand == null) { return 0; }
		return targetcommand.getInvokingArgumentCount();
	}

	@Override
	public boolean isGenerated() {
		return true;
	}


}
