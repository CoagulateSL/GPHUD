package net.coagulate.GPHUD.Modules.Scripting.Language;

import java.io.Serial;

public class GSInvalidPopError extends GSInternalError {
	@Serial
    private static final long serialVersionUID = 1L;

	public GSInvalidPopError(final String message) {
		super(message);
	}

	public GSInvalidPopError(final String message,
							 final Throwable cause) {
		super(message, cause);
	}

}
