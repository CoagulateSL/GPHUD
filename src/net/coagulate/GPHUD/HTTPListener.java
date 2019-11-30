package net.coagulate.GPHUD;

import org.apache.http.config.SocketConfig;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.util.logging.Level.SEVERE;

/**
 * listens on a port for forwarded connections from apache.
 * we register some handlers for "Page"s (see the GPHUD.User package) and reset and endpoint for the system interface (GPHUD.System).
 * Everything else goes to a HTML 404 handler.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class HTTPListener {
	@Nullable
	private static HttpServer server = null;
	private static boolean hasshutdown = false;

	public static void initialise(int port) {
		Runtime.getRuntime().addShutdownHook(new ShutdownHook());
		try {
			// start creating a server, on the port.  disable keepalive.  probably can get rid of that.
			SocketConfig reuse = SocketConfig.custom().
					setBacklogSize(100).
					setSoReuseAddress(true).build();

			final ServerBootstrap bootstrap = ServerBootstrap.bootstrap()
					.setListenerPort(port)
					.setSocketConfig(reuse)
					.setServerInfo("GPHUD/1.1")
					.setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE);
			// NOTE HOW THE HANDLERS ARE A SINGLE INSTANCE.
			// no instance level data storage.  USE HTTPCONTEXT (superceeded by "State")

			bootstrap.registerHandler("/system", new net.coagulate.GPHUD.Interfaces.System.Interface());
			bootstrap.registerHandler("/*", new net.coagulate.GPHUD.Interfaces.User.Interface());
			bootstrap.registerHandler("/shutdown", new net.coagulate.GPHUD.UnauthenticatedShutdown());
			server = bootstrap.create();
			GPHUD.getLogger().config("HTTP Services starting up on port " + port);
			server.start();
		} catch (IOException e) {
			// "whoops"
			GPHUD.getLogger().log(SEVERE, "Listener crashed", e);
			System.exit(3);
		}
	}

	public static void shutdown() {
		boolean logging = !hasshutdown;
		hasshutdown = true;
		if (server != null) {
			if (logging) { GPHUD.getLogger().info("Stopping listener gracefully"); }
			server.shutdown(3, TimeUnit.SECONDS);
			if (logging) { GPHUD.getLogger().info("Terminating listener"); }
			server.stop();
		}
	}

	public static void shutdownNow() {
		GPHUD.getLogger().warning("Shutdown hook invoked");
		server.shutdown(0, TimeUnit.NANOSECONDS);
		server.stop();
	}

	private static class ShutdownHook extends Thread {
		public void run() {
			HTTPListener.shutdown();
		}
	}
}
