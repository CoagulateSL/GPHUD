package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;

public class GSCastException extends GSInvalidExpressionException {
	private static final long serialVersionUID=1L;
	public GSCastException(String reason) {
		super(reason);
	}

	public GSCastException(String reason, Throwable cause) {
		super(reason, cause);
	}

	@Nonnull
	public String toString() { return "{GS Cast Exception}: "+getLocalizedMessage(); }
}
