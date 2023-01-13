package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;
import java.io.Serial;

public class GSInvalidExpressionException extends GSException {
	@Serial private static final long serialVersionUID=1L;
	
	public GSInvalidExpressionException(final String message) {
		super(message);
	}
	
	public GSInvalidExpressionException(final String reason,final boolean suppresslogging) {
		super(reason,suppresslogging);
	}
	
	public GSInvalidExpressionException(final String reason,final Throwable cause,final boolean suppresslogging) {
		super(reason,cause,suppresslogging);
	}
	
	public GSInvalidExpressionException(final String message,final Throwable cause) {
		super(message,cause);
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	public String toString() {
		return "{GS Invalid Expression Exception}: "+getLocalizedMessage();
	}
}
