package net.coagulate.GPHUD.Modules.Scripting.Language;

import java.io.Serial;

public class GSMathsError extends GSExecutionException {
	@Serial private static final long serialVersionUID=1L;
	
	public GSMathsError(final String message) {
		super(message);
	}
	
	public GSMathsError(final String message,final Throwable cause) {
		super(message,cause);
	}
	
	// ---------- INSTANCE ----------
	public String toString() {
		return "{GS Maths Error}: "+getLocalizedMessage();
	}
}
