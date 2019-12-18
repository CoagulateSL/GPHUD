package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.Modules.Argument.ArgumentType;
import net.coagulate.GPHUD.Modules.Argument.Arguments;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
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

	public CommandAnnotation(final Module owner, @Nonnull final Method c) {
		//System.out.println(owner);
		//System.out.println(c);
		this.owner = owner;
		method = c;
		meta = c.getAnnotation(Commands.class);
		validate(null);
		populateArguments();
		generated = false;
	}

	protected static void checkPublicStatic(@Nonnull final Method m) {
		if (!Modifier.isStatic(m.getModifiers())) {
			throw new SystemImplementationException("Method " + m.getDeclaringClass().getName() + "/" + m.getName() + " must be static");
		}
		if (!Modifier.isPublic(m.getModifiers())) {
			throw new SystemImplementationException("Method " + m.getDeclaringClass().getName() + "/" + m.getName() + " must be public");
		}
	}

	@Nonnull
	public Method getMethod() { return method; }

	public boolean isGenerated() { return generated; }

	void validate(final State st) {
		if (!requiresPermission().isEmpty()) {
			Modules.validatePermission(st, requiresPermission());
		}
		Module.checkPublicStatic(method);
		if (method.getParameterCount() == 0) {
			throw new SystemImplementationException("Method " + getFullName() + "() takes zero arguments but must take 'State' as its first argument");
		}
		if ((method.getParameters()[0]).getClass().getCanonicalName().equalsIgnoreCase(State.class.getCanonicalName())) {
			throw new SystemImplementationException("Method " + getFullName() + " must take State as its first argument");
		}
		for (int i = 1; i < method.getParameters().length; i++) {
			final Parameter p = method.getParameters()[i];
			final Arguments arg = p.getAnnotation(Arguments.class);
			if (arg == null) {
				throw new SystemImplementationException("Method " + getFullName() + "() argument " + (i + 1) + " (" + p.getName() + ") has no @Argument metadata");
			}
			if (arg.type() == ArgumentType.CHOICE) {
				// validate the choice method
				final String choicemethod = arg.choiceMethod();
				try {
					method.getDeclaringClass().getMethod(choicemethod, State.class);
				} catch (@Nonnull final Exception e) {
					throw new SystemImplementationException("Failed to instansiate choice method " + getFullName() + " / " + choicemethod);
				}
			}
		}

	}

	@Nonnull
	public String description() { return meta.description(); }

	@Nonnull
	public String requiresPermission() { return meta.requiresPermission(); }

	@Nonnull
	public Context context() { return meta.context(); }

	public boolean permitJSON() { return meta.permitJSON(); }

	public boolean permitConsole() { return meta.permitConsole(); }

	public boolean permitUserWeb() { return meta.permitUserWeb(); }

	public boolean permitObject() { return meta.permitObject(); }

	public boolean permitScripting() { return meta.permitScripting(); }

	@Nonnull
	public List<Argument> getArguments() { return arguments; }

	public int getArgumentCount() { return getArguments().size(); }

	@Nonnull
	public String getFullName() { return owner.getName() + "." + getName(); }

	@Nonnull
	public String getName() { return method.getName(); }

	private void populateArguments() {
		arguments = new ArrayList<>();
		boolean skipfirst = true; // first should be STATE
		for (final Parameter p : method.getParameters()) {
			if (skipfirst) { skipfirst = false; } else { arguments.add(new ArgumentAnnotation(this, p)); }
		}
	}

	/**
	 * Get the name of the arguments.
	 *
	 * @param st state
	 * @return list of argument names
	 */
	@Nonnull
	public List<String> getArgumentNames(final State st) {
		final List<String> arguments = new ArrayList<>();
		for (final Argument a : getArguments()) {
			arguments.add(a.getName());
		}
		return arguments;
	}

	@Nonnull
	public List<Argument> getInvokingArguments() { return getArguments(); }

	@Nonnull
	public String getFullMethodName() {
		return method.getDeclaringClass().getName() + "." + method.getName() + "()";
	}

	public int getInvokingArgumentCount() {
		return getArgumentCount();
	}


}
