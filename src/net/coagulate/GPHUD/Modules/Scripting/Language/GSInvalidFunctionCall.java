package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;

public class GSInvalidFunctionCall extends GSException {
	private static final long serialVersionUID=1L;

	public GSInvalidFunctionCall(final String reason) {
		super(reason);
	}

	public GSInvalidFunctionCall(final String reason,
	                             final Throwable cause) {
		super(reason,cause);
	}

	public GSInvalidFunctionCall(String reason, boolean suppresslogging) {
		super(reason, suppresslogging);
	}

	public GSInvalidFunctionCall(String reason, Throwable cause, boolean suppresslogging) {
		super(reason, cause, suppresslogging);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	public String toString() { return "{GS Invalid Function Call Exception}: "+getLocalizedMessage(); }
}
