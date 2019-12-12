package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.GPHUD.State;

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
	String overridedescription = null;
	private boolean generated = true;

	protected ArgumentAnnotation() {}

	public ArgumentAnnotation(Command c, Parameter p) {
		this.parameter = p;
		this.command = c;
		meta = p.getAnnotation(Arguments.class);
		generated = false;
	}

	public boolean isGenerated() { return generated; }

	public ArgumentType type() { return meta.type(); }

	public void overrideDescription(String n) { overridedescription = n; }

	public String description() {
		if (overridedescription != null) { return overridedescription; }
		return meta.description();
	}

	public boolean mandatory() { return meta.mandatory(); }

	public String choiceMethod() { return meta.choiceMethod(); }

	public Class<? extends Object> objectType() { return parameter.getType(); }

	public String getName() { return parameter.getName(); }

	public boolean delayTemplating() { return meta.delayTemplating(); }

	public int max() { return meta.max(); }

	@SuppressWarnings("unchecked")
	public List<String> getChoices(State st) throws SystemException {
		try {
			Method m = command.getMethod().getDeclaringClass().getMethod(choiceMethod(),State.class);
			return (List<String>) m.invoke(null, new Object[]{st});
		} catch (IllegalAccessException ex) {
			throw new SystemException("Access modifier problem loading choices from " + command.getFullName() + "/" + choiceMethod() + "()", ex);
		} catch (IllegalArgumentException ex) {
			throw new SystemException("Argument problem loading choices from " + command.getFullName() + "/" + choiceMethod() + "()", ex);
		} catch (InvocationTargetException ex) {
			throw new SystemException("Target method problem loading choices from " + command.getFullName() + "/" + choiceMethod() + "()", ex);
		} catch (NoSuchMethodException ex) {
			throw new SystemException("No such method problem loading choices from " + command.getFullName() + "/" + choiceMethod() + "()", ex);
		} catch (SecurityException ex) {
			throw new SystemException("Security problem loading choices from " + command.getFullName() + "/" + choiceMethod() + "()", ex);
		}
	}

}
