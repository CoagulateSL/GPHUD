package net.coagulate.GPHUD;

import net.coagulate.GPHUD.Modules.URL.URLs;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import javax.annotation.Nonnull;

import static java.util.logging.Level.SEVERE;

// sample primitive handler :P
public class UnauthenticatedShutdown implements HttpRequestHandler {

	@URLs(url="/shutdown")
	public void handle(final HttpRequest hr,
	                   final HttpResponse hr1,
	                   final HttpContext hc)
	{
		if (GPHUD.DEV) {
			GPHUD.getLogger().log(SEVERE,"UNAUTHENTICATED SHUTDOWN HANDLER IS ACTIVE AND HAS BEEN CALLED");
			HTTPListener.shutdown();
			try { Thread.sleep(1000); } catch (@Nonnull final InterruptedException e) {}
			GPHUD.getLogger().log(SEVERE,"Terminating execution NOW!");
			System.exit(2);
		}
	}

}
