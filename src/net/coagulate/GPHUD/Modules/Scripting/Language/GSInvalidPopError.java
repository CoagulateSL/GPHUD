package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSInvalidPopError extends GSInternalError {
	private static final long serialVersionUID=1L;

	public GSInvalidPopError(final String reason) {
		super(reason);
	}

	public GSInvalidPopError(final String reason,
	                         final Throwable cause) {
		super(reason,cause);
	}

}
