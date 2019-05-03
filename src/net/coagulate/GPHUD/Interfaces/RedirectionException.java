package net.coagulate.GPHUD.Interfaces;

import net.coagulate.GPHUD.SafeMap;

/**
 * Not really an exception, causes the interface to redirect the HTTP request to a new page.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class RedirectionException extends RuntimeException {
	String url;

	public RedirectionException(SafeMap values) {
		this(values.get("okreturnurl"));
	}

	public RedirectionException(String url) {
		super("Redirecting to " + url);
		this.url = url;
	}

	public String getURL() { return url; }
}
