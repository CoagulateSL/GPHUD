package net.coagulate.GPHUD.Interfaces;

import net.coagulate.GPHUD.SafeMap;

import javax.annotation.Nonnull;
import java.io.Serial;

/**
 * Not really an exception, causes the interface to redirect the HTTP request to a new page.
 *
 * @author Iain Price <gphud@predestined.net>
 */
public class RedirectionException extends RuntimeException {
	@Serial
    private static final long serialVersionUID=1L;
	final String url;

	public RedirectionException(@Nonnull final SafeMap values) {
		this(values.get("okreturnurl"));
	}

    @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
    public RedirectionException(final String url) {
        super("Redirecting to "+url);
        this.url=url;
    }

	// ---------- INSTANCE ----------
	public String getURL() { return url; }
}
