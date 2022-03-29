package net.coagulate.GPHUD.Interfaces;

import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.Core.HTTP.URLMapper;
import net.coagulate.GPHUD.State;
import net.coagulate.SL.Config;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Superclass of both interfaces.  Defines how we pass things down.
 * <p>
 * Does some basic connection agnostic stuff.  Note we just stash almost everything into the State since that's the "local scope" for the request.
 * DON'T USE CLASS LEVEL VARIABLES, the class is NOT instantiated per-request, but once for the whole system.  Pretend as if everything is "static"
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Interface extends URLMapper<Method> {

	private static final Map<Thread,State> threadState=new ConcurrentHashMap<>();

	@Nullable
	public static String base;

	// ---------- STATICS ----------
	public static String getNode() { return Config.getHostName(); }

	@Nonnull
	public static String base() {
		if (base==null) {
			base="GPHUD";
		}
		return base;
	}

	// we don't really know about "/app" but apache does, and then hides it from us, which is both nice, and arbitrary, either way really.
	// it doesn't any more :)
	@Nonnull
	public static String generateURL(final State st,
	                                 final String ending) {
		return "https://"+Config.getURLHost()+"/"+base()+"/"+ending;
	}

	public static int convertVersion(@Nonnull final String version) {
		final String[] versionParts=version.split("\\.");
		final int major=Integer.parseInt(versionParts[0]);
		final int minor=Integer.parseInt(versionParts[1]);
		final int bugfix=Integer.parseInt(versionParts[2]);
		if (major>99) { throw new SystemBadValueException("Major version number too high"); }
		if (minor>99) { throw new SystemBadValueException("Minor version number too high"); }
		if (bugfix>99) { throw new SystemBadValueException("Bugfix version number too high"); }
		return 10000*major+100*minor+bugfix;
	}

	protected State state() { return threadState.get(Thread.currentThread()); }
	@Override
	protected void earlyInitialiseState(HttpRequest request, HttpContext context) {
		super.earlyInitialiseState(request,context);
		threadState.put(Thread.currentThread(),new State(request,context));
	}

	@Override
	protected Method lookupPageFromUri(String line) {
		final State st=state();
		st.setURL(line);
		return super.lookupPageFromUri(line);
	}

	@Override
	protected void initialiseState(HttpRequest request, HttpContext context, Map<String, String> parameters, Map<String, String> cookies) {
	}

	@Override
	protected void loadSession() {

	}

	@Override
	protected boolean checkAuthenticationNeeded(Method content) {
		return false;
	}

	@Override
	protected void executePage(Method content) {

	}

	@Override
	protected void cleanup() {
		threadState.remove(Thread.currentThread());
	}
}
