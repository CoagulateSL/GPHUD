package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.SystemException;

import javax.annotation.Nonnull;

public class GSInternalError extends SystemException {
	private static final long serialVersionUID=1L;

	public GSInternalError(final String reason) {
		super(reason);
	}

	public GSInternalError(final String reason,
	                       final Throwable cause) {
		super(reason,cause);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	public String toString() { return "{GS Internal Error}: "+getLocalizedMessage(); }
}
