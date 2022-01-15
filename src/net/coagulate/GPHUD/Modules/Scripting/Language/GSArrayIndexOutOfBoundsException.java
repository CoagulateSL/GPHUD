package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;

public class GSArrayIndexOutOfBoundsException extends GSInvalidExpressionException {
	private static final long serialVersionUID=1L;

	public GSArrayIndexOutOfBoundsException(final String reason) {
		super(reason);
	}

	public GSArrayIndexOutOfBoundsException(String reason, boolean suppresslogging) {
		super(reason, suppresslogging);
	}

	public GSArrayIndexOutOfBoundsException(final String reason,
											final Throwable cause) {
		super(reason,cause);
	}

	// ---------- INSTANCE ----------
	@Nonnull
	public String toString() { return "{GS Array Index Out Of Bounds Exception}: "+getLocalizedMessage(); }

}
