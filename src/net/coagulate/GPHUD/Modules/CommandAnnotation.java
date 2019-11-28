package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.State;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * A command, probably derived from Annotations.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class CommandAnnotation extends Command {
	Module owner;
	Commands meta;
	Method method;
	List<Argument> arguments;
	private boolean generated = true;

	protected CommandAnnotation() {}

	public CommandAnnotation(Module owner, Method c) throws SystemException, UserException {
		//System.out.println(owner);
		//System.out.println(c);
		this.owner = owner;
		this.method = c;
		this.meta = c.getAnnotation(Commands.class);
		validate(null);
		populateArguments();
		generated = false;
	}

	protected static void checkPublicStatic(Method m) throws SystemException {
		if (!Modifier.isStatic(m.getModifiers())) {
			throw new SystemException("Method " + m.getDeclaringClass().getName() + "/" + m.getName() + " must be static");
		}
		if (!Modifier.isPublic(m.getModifiers())) {
			throw new SystemException("Method " + m.getDeclaringClass().getName() + "/" + m.getName() + " must be public");
		}
	}

	public Method getMethod() { return method; }

	public boolean isGenerated() { return generated; }

	void validate(State st) throws SystemException, UserException {
		if (!requiresPermission().isEmpty()) {
			Modules.validatePermission(st, requiresPermission());
		}
		Module.checkPublicStatic(method);
		if (method.getParameterCount() == 0) {
			throw new SystemException("Method " + getFullName() + "() takes zero arguments but must take 'State' as its first argument");
		}
		if ((method.getParameters()[0]).getClass().getCanonicalName().equalsIgnoreCase(State.class.getCanonicalName())) {
			throw new SystemException("Method " + getFullName() + " must take State as its first argument");
		}
		for (int i = 1; i < method.getParameters().length; i++) {
			Parameter p = method.getParameters()[i];
			Arguments arg = p.getAnnotation(Arguments.class);
			if (arg == null) {
				throw new SystemException("Method " + getFullName() + "() argument " + (i + 1) + " (" + p.getName() + ") has no @Argument metadata");
			}
			if (arg.type() == ArgumentType.CHOICE) {
				// validate the choice method
				String choicemethod = arg.choiceMethod();
				try {
					method.getDeclaringClass().getMethod(choicemethod, new Class[]{State.class});
				} catch (Exception e) {
					throw new SystemException("Failed to instansiate choice method " + getFullName() + " / " + choicemethod);
				}
			}
		}

	}

	public String description() { return meta.description(); }

	public String requiresPermission() { return meta.requiresPermission(); }

	public Context context() { return meta.context(); }

	public boolean permitJSON() { return meta.permitJSON(); }

	public boolean permitConsole() { return meta.permitConsole(); }

	public boolean permitUserWeb() { return meta.permitUserWeb(); }

	public boolean permitObject() { return meta.permitObject(); }

	public boolean permitScripting() { return meta.permitScripting(); }

	public List<Argument> getArguments() { return arguments; }

	public int getArgumentCount() { return getArguments().size(); }

	public String getFullName() { return owner.getName() + "." + getName(); }

	public String getName() { return method.getName(); }

	private void populateArguments() {
		arguments = new ArrayList<>();
		boolean skipfirst = true; // first should be STATE
		for (Parameter p : method.getParameters()) {
			if (skipfirst) { skipfirst = false; } else { arguments.add(new ArgumentAnnotation(this, p)); }
		}
	}

	/**
	 * Get the name of the arguments.
	 *
	 * @param st
	 * @return
	 * @throws UserException
	 */
	public List<String> getArgumentNames(State st) throws UserException {
		List<String> arguments = new ArrayList<>();
		for (Argument a : getArguments()) {
			arguments.add(a.getName());
		}
		return arguments;
	}

	public List<Argument> getInvokingArguments() { return getArguments(); }

	public String getFullMethodName() {
		return method.getDeclaringClass().getName() + "." + method.getName() + "()";
	}

	public int getInvokingArgumentCount() {
		return getArgumentCount();
	}


}
