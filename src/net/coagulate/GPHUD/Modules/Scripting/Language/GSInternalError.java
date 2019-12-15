package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.SystemException;

import javax.annotation.Nonnull;

public class GSInternalError extends SystemException {
	private static final long serialVersionUID=1L;
	public GSInternalError(String reason) {
		super(reason);
	}

	public GSInternalError(String reason, Throwable cause) {
		super(reason, cause);
	}

	@Nonnull
	public String toString() { return "{GS Internal Error}: "+getLocalizedMessage(); }
}
