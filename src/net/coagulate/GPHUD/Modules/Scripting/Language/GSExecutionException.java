package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSExecutionException extends GSException {
	private static final long serialVersionUID = 1L;

	public GSExecutionException(final String message) {
		super(message);
	}

	public GSExecutionException(final String message,
								final Throwable cause) {
		super(message, cause);
	}

	// ---------- INSTANCE ----------
	public String toString() {
		return "{GS Execution Exception}: " + getLocalizedMessage();
	}
}
