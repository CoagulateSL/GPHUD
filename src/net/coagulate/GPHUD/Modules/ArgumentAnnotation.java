package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private boolean generated=true;

	protected ArgumentAnnotation() {}

	public ArgumentAnnotation(final Command c,
	                          @Nonnull final Parameter p) {
		parameter=p;
		command=c;
		meta=p.getAnnotation(Arguments.class);
		generated=false;
		choiceMethod();
		if (!choiceMethod().isEmpty()) { CommandAnnotation.checkPublicStatic(getMethod()); }
	}

	public Method getMethod() { return getMethod(choiceMethod()); }

	// ---------- STATICS ----------
	public static Method getMethod(final String fqn) {
		if (!fqn.contains(".")) { throw new SystemImplementationException("Non fully qualified method name in "+fqn); }
		final Matcher matcher=Pattern.compile("(.*)\\.([^.]*)").matcher(fqn);
		if (!matcher.matches()) { throw new SystemImplementationException("Regexp matcher failure for fq method "+fqn); }
		if (matcher.groupCount()!=2) { throw new SystemImplementationException("Qualified method name "+fqn+" broke into more than 2 parts"); }
		final String classname=matcher.group(1);
		final String methodname=matcher.group(2);
		final Class<?> clazz;
		try {
			clazz=Class.forName(classname);
		}
		catch (final ClassNotFoundException e) {
			throw new SystemImplementationException("FQN for choice method '"+fqn+"' gave class not found",e);
		}
		try {
			return clazz.getMethod(methodname,State.class);
		}
		catch (final NoSuchMethodException e) {
			throw new SystemImplementationException("FQN for choice method '"+fqn+"' gave method not found",e);
		}
	}

	// ---------- INSTANCE ----------
	public boolean isGenerated() { return generated; }

	@Nonnull
	public ArgumentType type() { return meta.type(); }

	@Nonnull
	public String description() {
		if (overridedescription!=null) { return overridedescription; }
		return meta.description();
	}

	public boolean mandatory() { return meta.mandatory(); }

	public Class<? extends Object> objectType() { return parameter.getType(); }

	public String getName() { return parameter.getName(); }

	public boolean delayTemplating() { return meta.delayTemplating(); }

	public int max() { return meta.max(); }

	public void overrideDescription(final String n) { overridedescription=n; }

	@Nonnull
	@SuppressWarnings("unchecked")
	public List<String> getChoices(final State st) {
		try {
			final Method m=getMethod();
			return (List<String>) m.invoke(null,new Object[]{st});
		}
		catch (@Nonnull final IllegalAccessException ex) {
			throw new SystemImplementationException("Access modifier problem loading choices from "+command.getFullName()+"/"+choiceMethod()+"()",ex);
		}
		catch (@Nonnull final IllegalArgumentException ex) {
			throw new SystemImplementationException("Argument problem loading choices from "+command.getFullName()+"/"+choiceMethod()+"()",ex);
		}
		catch (@Nonnull final InvocationTargetException ex) {
			throw new SystemImplementationException("Target method problem loading choices from "+command.getFullName()+"/"+choiceMethod()+"()",ex);
		}
		catch (@Nonnull final SecurityException ex) {
			throw new SystemImplementationException("Security problem loading choices from "+command.getFullName()+"/"+choiceMethod()+"()",ex);
		}
	}

	@Nonnull
	public String choiceMethod() { return meta.choiceMethod(); }

}
