package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;
import java.io.Serial;

public class GSArrayIndexOutOfBoundsException extends GSInvalidExpressionException {
	@Serial private static final long serialVersionUID=1L;
	
	public GSArrayIndexOutOfBoundsException(final String message) {
		super(message);
	}
	
	public GSArrayIndexOutOfBoundsException(final String reason,final boolean suppresslogging) {
		super(reason,suppresslogging);
	}
	
	public GSArrayIndexOutOfBoundsException(final String message,final Throwable cause) {
		super(message,cause);
	}
	
	// ---------- INSTANCE ----------
	@Nonnull
	public String toString() {
		return "{GS Array Index Out Of Bounds Exception}: "+getLocalizedMessage();
	}
	
}
