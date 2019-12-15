package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;

public class GSUnknownIdentifier extends GSException {
	private static final long serialVersionUID=1L;
	public GSUnknownIdentifier(final String reason) {
		super(reason);
	}

	public GSUnknownIdentifier(final String reason, final Throwable cause) {
		super(reason, cause);
	}

	@Nonnull
	public String toString() { return "{GS Unknown Identifier Exception}: "+getLocalizedMessage(); }
}
