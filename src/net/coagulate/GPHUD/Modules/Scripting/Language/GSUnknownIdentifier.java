package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;
import java.io.Serial;

public class GSUnknownIdentifier extends GSException {
    @Serial
    private static final long serialVersionUID = 1L;

    public GSUnknownIdentifier(final String message) {
        super(message);
    }

    public GSUnknownIdentifier(final String reason, final boolean suppresslogging) {
        super(reason, suppresslogging);
    }

    public GSUnknownIdentifier(final String message,
                               final Throwable cause) {
        super(message, cause);
    }

    public GSUnknownIdentifier(final String reason,
                               final Throwable cause,
                               final boolean suppresslogging) {
        super(reason, cause, suppresslogging);
    }

	// ---------- INSTANCE ----------
	@Nonnull
	public String toString() { return "{GS Unknown Identifier Exception}: "+getLocalizedMessage(); }
}
