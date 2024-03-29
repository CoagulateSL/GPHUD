package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;
import java.io.Serial;

public class GSCastException extends GSInvalidExpressionException {
	@Serial private static final long serialVersionUID=1L;
	
	public GSCastException(final String message) {
		super(message);
	}
	
	public GSCastException(final String reason,final boolean suppresslogging) {
		super(reason,suppresslogging);
	}
	
	public GSCastException(final String message,final Throwable cause) {
		super(message,cause);
	}
	
	public GSCastException(final String reason,final Throwable cause,final boolean suppresslogging) {
		super(reason,cause,suppresslogging);
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	public String toString() {
		return "{GS Cast Exception}: "+getLocalizedMessage();
	}
}
