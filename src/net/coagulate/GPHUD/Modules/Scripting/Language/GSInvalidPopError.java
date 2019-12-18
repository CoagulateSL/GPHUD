package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;

public class GSInvalidPopError extends GSInternalError {
	private static final long serialVersionUID=1L;

	public GSInvalidPopError(final String reason) {
		super(reason);
	}

	public GSInvalidPopError(final String reason,
	                         final Throwable cause)
	{
		super(reason,cause);
	}

	@Nonnull
	public String toString() { return "{GS Internal Error}: "+getLocalizedMessage(); }
}
