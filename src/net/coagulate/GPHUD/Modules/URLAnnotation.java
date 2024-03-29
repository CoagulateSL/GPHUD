package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Exceptions.System.SystemImplementationException;
import net.coagulate.Core.Exceptions.SystemException;
import net.coagulate.Core.Exceptions.User.UserInputValidationParseException;
import net.coagulate.Core.Exceptions.UserException;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Wraps a URL.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class URLAnnotation extends URL {
	private static final   Set<String> inuse=new HashSet<>(); // a legitimate static in a non static class oO
	private final          URLs        meta;
	private final          Module      module;
	@Nonnull private final Method      method;
	private final          boolean     generated;
	
	public URLAnnotation(final Module module,@Nonnull final Method method) {
		this.module=module;
		this.method=method;
		meta=method.getAnnotation(URLs.class);
		checkUnique();
		validate(null);
		generated=false;
	}
	
	public void checkUnique() {
		if (inuse.contains(url())) {
			throw new SystemImplementationException("URL "+url()+" is already claimed");
		}
		inuse.add(url());
	}
	
	// ----- Internal Instance -----
	private void validate(final State st) {
		if (!requiresPermission().isEmpty()) {
			Modules.validatePermission(st,requiresPermission());
		}
		Module.checkPublicStatic(method);
		final Class<?>[] params=method.getParameterTypes();
		final String fullname=method.getDeclaringClass().getName()+"."+method.getName();
		if (params.length!=2) {
			throw new SystemImplementationException(
					"Method "+fullname+" must have 2 arguments (State,SafeMap) but has "+params.length);
		}
		if (params[0]!=State.class) {
			throw new SystemImplementationException("Method "+fullname+" must have State as its first argument");
		}
		if (params[1]!=SafeMap.class) {
			throw new SystemImplementationException("Method "+fullname+" must have SafeMap as its second argument");
		}
		
	}
	
	// ---------- INSTANCE ----------
	public boolean isGenerated() {
		return generated;
	}
	
	@Override
	public boolean requiresAuthentication() {
		return meta.requiresAuthentication();
	}
	
	@Nonnull
	public String url() {
		return meta.url();
	}
	
	@Nonnull
	public String requiresPermission() {
		return meta.requiresPermission();
	}
	
	@Nonnull
	public String getMethodName() {
		return method.getDeclaringClass().getName()+"."+method.getName()+"()";
	}
	
	@Nonnull
	public String getFullName() {
		return module.getName()+"."+getName();
	}
	
	public String getName() {
		return method.getName();
	}
	
	public void run(@Nonnull final State st,final SafeMap values) {
		try {
			method.invoke(null,st,values);
		} catch (@Nonnull final IllegalAccessException ex) {
			throw new SystemImplementationException("Illegal method access to content at "+st.getDebasedURL(),ex);
		} catch (@Nonnull final IllegalArgumentException ex) {
			throw new SystemImplementationException(
					"Illegal arguments on content, should be (State,Map<String,String>), at "+st.getDebasedURL(),
					ex);
		} catch (@Nonnull final InvocationTargetException ex) {
			final Throwable contained=ex.getCause();
			if (contained instanceof RedirectionException) { //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
				throw (RedirectionException)contained;
			}
			if (SystemException.class.isAssignableFrom(contained.getClass())) { //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
				throw (SystemException)contained;
			}
			if (UserException.class.isAssignableFrom(contained.getClass())) { //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
				throw (UserException)contained;
			}
			if (contained instanceof NumberFormatException) {
				//noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
				throw new UserInputValidationParseException("Number Format Exception",contained);
			}
			if (contained instanceof NullPointerException) {
				//noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
				throw new SystemImplementationException("Null Pointer Exception",contained);
			}
			//noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
			throw new SystemImplementationException("Non GPHUD exception inside page at "+st.getDebasedURL(),contained);
		}
	}
	
	@Override
	public Module getModule() {
		return module;
	}
	
	@Nonnull
	public Method getMethod() {
		return method;
	}
}
