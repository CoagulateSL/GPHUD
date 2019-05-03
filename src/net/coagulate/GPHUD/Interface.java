package net.coagulate.GPHUD;

import net.coagulate.SL.SL;
import org.apache.http.Header;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.net.InetAddress;

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

	public static String base = null;

	public static String getNode() { return GPHUD.hostname; }

	public static String base() {
		if (base == null) {
			if (Interface.getNode().equalsIgnoreCase("luna")) { base = "app-iain"; } else { base = "app"; }
		}
		return base;
	}

	// we dont really know about "/app" but apache does, and then hides it from us, which is both nice, and arbitary, either way really.
	// it doesn't any more :)
	public static String generateURL(State st, String ending) {
		return "https://sl" + (GPHUD.DEV ? "dev" : "") + ".coagulate.net/" + base() + "/" + ending;
	}

	@Override
	public void handle(HttpRequest req, HttpResponse resp, HttpContext httpcontext) {
		State st = new State(req, resp, httpcontext);
		try {
			// create our state object

			// get remote address.  Probably always localhost :P
			@SuppressWarnings("deprecation")
			HttpInetConnection connection = (HttpInetConnection) httpcontext.getAttribute(org.apache.http.protocol.ExecutionContext.HTTP_CONNECTION);
			InetAddress ia = connection.getRemoteAddress();

			// the requested URI
			String uri = req.getRequestLine().getUri();

			// get requested Host: from the headers.
			Header headers[] = req.getAllHeaders();
			String host = "";
			for (Header h : headers) {
				if (h.getName().equals("Host")) {host = h.getValue(); }
			}

			// we use the HOST to construct absolute URLs so it's somewhat important :P
			if (host.equals("")) { GPHUD.getLogger().warning("Host accessed is not known :("); }

			// stash the remote address, uri, the headers array, request and response in the context
			//System.out.println(req.getRequestLine().toString());
			st.address = ia;
			st.setURL(uri);
			st.headers = headers;
			st.host = host;
			st.handler = this;

			// process the page request
			process(st);
		} catch (Exception e) {
			// there is no INTENDED use case for this catch, each interface should have its own 'catchall' somewhere, but just in case
			SL.report("Generic Interface caught exception", e, st);
			GPHUD.getLogger().log(SEVERE, "Exception propagated to generic interface handler!", e);
		}
	}

	// implemented by the different interfaces
	public abstract void process(State st);


}
