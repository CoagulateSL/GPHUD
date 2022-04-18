package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSInvalidPopError extends GSInternalError {
	private static final long serialVersionUID = 1L;

	public GSInvalidPopError(final String message) {
		super(message);
	}

	public GSInvalidPopError(final String message,
							 final Throwable cause) {
		super(message, cause);
	}

}
