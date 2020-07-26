package net.coagulate.GPHUD;

import net.coagulate.Core.Exceptions.System.SystemBadValueException;
import net.coagulate.SL.Config;
import net.coagulate.SL.SL;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.util.logging.Level.SEVERE;

/**
 * Superclass of both interfaces.  Defines how we pass things down.
 * <p>
 * Does some basic connection agnostic stuff.  Note we just stash almost everything into the State since thats the "local scope" for the request.
 * DONT USE CLASS LEVEL VARIABLES, the class is NOT instantiated per-request, but once for the whole system.  Pretend as if everything is "static"
 *
 * @author Iain Price <gphud@predestined.net>
 */
public abstract class Interface implements HttpRequestHandler {

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

	// we dont really know about "/app" but apache does, and then hides it from us, which is both nice, and arbitary, either way really.
	// it doesn't any more :)
	@Nonnull
	public static String generateURL(final State st,
	                                 final String ending) {
		return "https://"+(Config.getDevelopment()?"dev.":"")+"sl.coagulate.net/"+base()+"/"+ending;
	}

	public static int convertVersion(@Nonnull final String version) {
		final String[] versionparts=version.split("\\.");
		final int major=Integer.parseInt(versionparts[0]);
		final int minor=Integer.parseInt(versionparts[1]);
		final int bugfix=Integer.parseInt(versionparts[2]);
		if (major>99) { throw new SystemBadValueException("Major version number too high"); }
		if (minor>99) { throw new SystemBadValueException("Minor version number too high"); }
		if (bugfix>99) { throw new SystemBadValueException("Bugfix version number too high"); }
		return 10000*major+100*minor+bugfix;
	}

	// ---------- INSTANCE ----------
	@Override
	public void handle(@Nonnull final HttpRequest req,
	                   final HttpResponse resp,
	                   @Nonnull final HttpContext httpcontext) {
		final State st=new State(req,resp,httpcontext);
		try {
			// create our state object

			// the requested URI
			final String uri=req.getRequestLine().getUri();

			// get requested Host: from the headers.
			final Header[] headers=req.getAllHeaders();
			String host="";
			for (final Header h: headers) {
				if ("Host".equals(h.getName())) {host=h.getValue(); }
			}

			// we use the HOST to construct absolute URLs so it's somewhat important :P
			if ("".equals(host)) { GPHUD.getLogger().warning("Host accessed is not known :("); }

			// stash the remote address, uri, the headers array, request and response in the context
			//System.out.println(req.getRequestLine().toString());
			st.setURL(uri);
			st.headers(headers);
			st.host=host;
			// process the page request
			process(st);
		}
		catch (@Nonnull final Exception e) {
			// there is no INTENDED use case for this catch, each interface should have its own 'catchall' somewhere, but just in case
			SL.report("Generic Interface caught exception",e,st);
			GPHUD.getLogger().log(SEVERE,"Exception propagated to generic interface handler!",e);
		}
	}

	// implemented by the different interfaces
	public abstract void process(State st);


}
