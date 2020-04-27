package net.coagulate.GPHUD.Interfaces.External;

import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;

public class ExternalInterfaceObjectAccessDeniedException extends UserAccessDeniedException {
	private static final long serialVersionUID=1L;

	public ExternalInterfaceObjectAccessDeniedException(final String reason) {
		super(reason);
	}

	public ExternalInterfaceObjectAccessDeniedException(final String reason,final Throwable cause) {
		super(reason,cause);
	}
}
