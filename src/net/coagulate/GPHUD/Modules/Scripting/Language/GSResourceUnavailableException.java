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

	@Nonnull
	public String toString() { return "{GS Resource Unavailable Exception}: "+getLocalizedMessage(); }
}
