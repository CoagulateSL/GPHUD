package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSExecutionException extends GSException {
	private static final long serialVersionUID=1L;

	public GSExecutionException(final String reason) {
		super(reason);
	}

	public GSExecutionException(final String reason,
	                            final Throwable cause) {
		super(reason,cause);
	}

	// ---------- INSTANCE ----------
	public String toString() { return "{GS Execution Exception}: "+getLocalizedMessage(); }
}
