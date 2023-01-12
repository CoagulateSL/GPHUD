package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;
import java.io.Serial;

public class GSInvalidFunctionCall extends GSException {
	@Serial private static final long serialVersionUID=1L;
	
	public GSInvalidFunctionCall(final String message) {
		super(message);
	}
	
	public GSInvalidFunctionCall(final String message,final Throwable cause) {
		super(message,cause);
	}
	
	public GSInvalidFunctionCall(final String reason,final boolean suppresslogging) {
		super(reason,suppresslogging);
	}
	
	public GSInvalidFunctionCall(final String reason,final Throwable cause,final boolean suppresslogging) {
		super(reason,cause,suppresslogging);
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	public String toString() {
		return "{GS Invalid Function Call Exception}: "+getLocalizedMessage();
	}
}
