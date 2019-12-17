package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

/**
 * Wraps an argument.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class ArgumentAnnotation extends Argument {
	Arguments meta;
	Parameter parameter;
	Command command;
	@Nullable
	String overridedescription;
	private boolean generated = true;

	protected ArgumentAnnotation() {}

	public ArgumentAnnotation(final Command c, @Nonnull final Parameter p) {
		parameter = p;
		command = c;
		meta = p.getAnnotation(Arguments.class);
		generated = false;
	}

	public boolean isGenerated() { return generated; }

	@Nonnull
	public ArgumentType type() { return meta.type(); }

	public void overrideDescription(final String n) { overridedescription = n; }

	@Nonnull
	public String description() {
		if (overridedescription != null) { return overridedescription; }
		return meta.description();
	}

	public boolean mandatory() { return meta.mandatory(); }

	@Nonnull
	public String choiceMethod() { return meta.choiceMethod(); }

	public Class<? extends Object> objectType() { return parameter.getType(); }

	public String getName() { return parameter.getName(); }

	public boolean delayTemplating() { return meta.delayTemplating(); }

	public int max() { return meta.max(); }

	@Nonnull
	@SuppressWarnings("unchecked")
	public List<String> getChoices(final State st) throws SystemException {
		try {
			final Method m = command.getMethod().getDeclaringClass().getMethod(choiceMethod(),State.class);
			return (List<String>) m.invoke(null, new Object[]{st});
		} catch (@Nonnull final IllegalAccessException ex) {
			throw new SystemImplementationException("Access modifier problem loading choices from " + command.getFullName() + "/" + choiceMethod() + "()", ex);
		} catch (@Nonnull final IllegalArgumentException ex) {
			throw new SystemImplementationException("Argument problem loading choices from " + command.getFullName() + "/" + choiceMethod() + "()", ex);
		} catch (@Nonnull final InvocationTargetException ex) {
			throw new SystemImplementationException("Target method problem loading choices from " + command.getFullName() + "/" + choiceMethod() + "()", ex);
		} catch (@Nonnull final NoSuchMethodException ex) {
			throw new SystemImplementationException("No such method problem loading choices from " + command.getFullName() + "/" + choiceMethod() + "()", ex);
		} catch (@Nonnull final SecurityException ex) {
			throw new SystemImplementationException("Security problem loading choices from " + command.getFullName() + "/" + choiceMethod() + "()", ex);
		}
	}

}
