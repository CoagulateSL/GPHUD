package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;

public class GSResourceUnavailableException extends GSException {
	private static final long serialVersionUID=1L;

	public GSResourceUnavailableException(final String reason) {
		super(reason);
	}

	public GSResourceUnavailableException(final String reason,
	                                      final Throwable cause) {
		super(reason,cause);
	}

	public GSResourceUnavailableException(String reason, boolean suppresslogging) {
		super(reason, suppresslogging);
	}

	public GSResourceUnavailableException(String reason, Throwable cause, boolean suppresslogging) {
		super(reason, cause, suppresslogging);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	public String toString() { return "{GS Resource Unavailable Exception}: "+getLocalizedMessage(); }
}
