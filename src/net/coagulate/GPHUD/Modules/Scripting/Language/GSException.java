package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.UserException;

import javax.annotation.Nonnull;

public abstract class GSException extends UserException {
	private static final long serialVersionUID=1L;
	public GSException(final String reason) {
		super(reason);
	}

	public GSException(final String reason, final Throwable cause) {
		super(reason, cause);
	}

	@Nonnull
	public abstract String toString();
}
