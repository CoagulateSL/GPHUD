package net.coagulate.GPHUD.Modules;

import net.coagulate.Core.Tools.SystemException;
import net.coagulate.Core.Tools.UserException;
import net.coagulate.GPHUD.Interfaces.RedirectionException;
import net.coagulate.GPHUD.SafeMap;
import net.coagulate.GPHUD.State;

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
	private static Set<String> inuse = new HashSet<>(); // a legitimate static in a non static class oO
	private URLs meta;
	private Module module;
	private Method method;
	private boolean generated = true;

	public URLAnnotation(Module module, Method method) throws SystemException, UserException {
		this.module = module;
		this.method = method;
		this.meta = method.getAnnotation(URLs.class);
		checkUnique();
		validate(null);
		generated = false;
	}

	public boolean isGenerated() { return generated; }

	public Method getMethod() { return method; }

	public String url() { return meta.url(); }

	public String requiresPermission() { return meta.requiresPermission(); }

	public String getFullName() { return module.getName() + "." + getName(); }

	public String getName() { return method.getName(); }

	public void checkUnique() throws SystemException {
		if (inuse.contains(url())) { throw new SystemException("URL " + url() + " is already claimed"); }
		inuse.add(url());
	}

	private void validate(State st) throws UserException, SystemException {
		if (!requiresPermission().isEmpty()) {
			Modules.validatePermission(st, requiresPermission());
		}
		Module.checkPublicStatic(method);
		Class<?>[] params = method.getParameterTypes();
		String fullname = method.getDeclaringClass().getName() + "." + method.getName();
		if (params.length != 2) {
			throw new SystemException("Method " + fullname + " must have 2 arguments (State,SafeMap) but has " + params.length);
		}
		if (params[0] != State.class) {
			throw new SystemException("Method " + fullname + " must have State as its first argument");
		}
		if (params[1] != SafeMap.class) {
			throw new SystemException("Method " + fullname + " must have SafeMap as its second argument");
		}

	}

	public void run(State st, SafeMap values) throws SystemException, UserException {
		try {
			method.invoke(null, new Object[]{st, values});
		} catch (IllegalAccessException ex) {
			throw new SystemException("Illegal method access to content at " + st.getDebasedURL(), ex);
		} catch (IllegalArgumentException ex) {
			throw new SystemException("Illegal arguments on content, should be (State,Map<String,String>), at " + st.getDebasedURL(), ex);
		} catch (InvocationTargetException ex) {
			Throwable contained = ex.getCause();
			if (contained instanceof RedirectionException) { throw (RedirectionException) contained; }
			if (contained instanceof SystemException) { throw (SystemException) contained; }
			if (contained instanceof UserException) { throw (UserException) contained; }
			if (contained instanceof NumberFormatException) {
				throw new UserException("Number Format Exception", contained);
			}
			if (contained instanceof NullPointerException) {
				throw new SystemException("Null Pointer Exception", contained);
			}
			throw new SystemException("Non GPHUD exception inside page at " + st.getDebasedURL(), contained);
		}
	}

	public String getMethodName() {
		return method.getDeclaringClass().getName() + "." + method.getName() + "()";
	}

	@Override
	public boolean requiresAuthentication() {
		return meta.requiresAuthentication();
	}
}
