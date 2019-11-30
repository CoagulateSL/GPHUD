package net.coagulate.GPHUD.Modules.Scripting.Language;

import javax.annotation.Nonnull;

public class GSResourceLimitExceededException extends GSException {
	private static final long serialVersionUID=1L;
	public GSResourceLimitExceededException(String reason) {
		super(reason);
	}

	public GSResourceLimitExceededException(String reason, Throwable cause) {
		super(reason, cause);
	}

	@Nonnull
	public String toString() { return "{GS Resource Limit Exceeded Exception}: "+getLocalizedMessage(); }
}
