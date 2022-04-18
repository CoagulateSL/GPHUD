package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSMathsError extends GSExecutionException {
    private static final long serialVersionUID = 1L;

    public GSMathsError(final String message) {
        super(message);
    }

    public GSMathsError(final String message,
                        final Throwable cause) {
        super(message, cause);
    }

    // ---------- INSTANCE ----------
    public String toString() {
        return "{GS Maths Error}: " + getLocalizedMessage();
    }
}
