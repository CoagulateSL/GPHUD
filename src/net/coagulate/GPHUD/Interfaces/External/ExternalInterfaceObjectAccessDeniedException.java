package net.coagulate.GPHUD.Interfaces.External;

import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;

public class ExternalInterfaceObjectAccessDeniedException extends UserAccessDeniedException {
	private static final long serialVersionUID = 1L;

	public ExternalInterfaceObjectAccessDeniedException(final String message) {
		super(message);
	}

	public ExternalInterfaceObjectAccessDeniedException(final String message,
														final Throwable cause) {
		super(message, cause);
	}
}
