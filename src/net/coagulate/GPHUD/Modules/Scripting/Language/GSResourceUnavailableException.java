package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;

public class GSResourceUnavailableException extends GSException {
    private static final long serialVersionUID = 1L;

    public GSResourceUnavailableException(final String message) {
        super(message);
    }

    public GSResourceUnavailableException(final String message,
                                          final Throwable cause) {
        super(message, cause);
    }

    public GSResourceUnavailableException(final String reason, final boolean suppresslogging) {
        super(reason, suppresslogging);
    }

    public GSResourceUnavailableException(final String reason, final Throwable cause, final boolean suppresslogging) {
        super(reason, cause, suppresslogging);
    }

	// ---------- INSTANCE ----------
	@Nonnull
	public String toString() { return "{GS Resource Unavailable Exception}: "+getLocalizedMessage(); }
}
