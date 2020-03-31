package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;

public class GSCastException extends GSInvalidExpressionException {
	private static final long serialVersionUID=1L;

	public GSCastException(final String reason) {
		super(reason);
	}

	public GSCastException(final String reason,
	                       final Throwable cause) {
		super(reason,cause);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	public String toString() { return "{GS Cast Exception}: "+getLocalizedMessage(); }
}
