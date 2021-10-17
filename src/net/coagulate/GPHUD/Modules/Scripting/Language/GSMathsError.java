package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSMathsError extends GSExecutionException {
	private static final long serialVersionUID=1L;

	public GSMathsError(final String reason) {
		super(reason);
	}

	public GSMathsError(final String reason,
                        final Throwable cause) {
		super(reason,cause);
	}

	// ---------- INSTANCE ----------
	public String toString() { return "{GS Maths Error}: "+getLocalizedMessage(); }
}
