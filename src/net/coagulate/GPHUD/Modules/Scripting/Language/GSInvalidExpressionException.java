package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;

public class GSInvalidExpressionException extends GSException {
	private static final long serialVersionUID=1L;

	public GSInvalidExpressionException(final String reason) {
		super(reason);
	}

	public GSInvalidExpressionException(final String reason, final boolean suppresslogging) {
        super(reason, suppresslogging);
    }

    public GSInvalidExpressionException(final String reason, final Throwable cause, final boolean suppresslogging) {
        super(reason, cause, suppresslogging);
    }

	public GSInvalidExpressionException(final String reason,
										final Throwable cause) {
		super(reason,cause);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	public String toString() { return "{GS Invalid Expression Exception}: "+getLocalizedMessage(); }
}
