package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.SystemException;

import javax.annotation.Nonnull;
import java.io.Serial;

public class GSInternalError extends SystemException {
	@Serial
    private static final long serialVersionUID = 1L;

	public GSInternalError(final String message) {
		super(message);
	}

	public GSInternalError(final String message,
						   final Throwable cause) {
		super(message, cause);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	public String toString() {
		return "{GS Internal Error}: " + getLocalizedMessage();
	}
}
