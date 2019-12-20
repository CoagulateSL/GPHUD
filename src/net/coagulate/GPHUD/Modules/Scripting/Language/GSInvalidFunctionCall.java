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

	@Nonnull
	public String toString() { return "{GS Invalid Function Call Exception}: "+getLocalizedMessage(); }
}
