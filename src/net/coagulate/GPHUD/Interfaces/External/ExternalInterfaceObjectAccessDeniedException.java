package net.coagulate.GPHUD.Interfaces.External;

import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;

public class ExternalInterfaceObjectAccessDeniedException extends UserAccessDeniedException {
	private static final long serialVersionUID=1L;

	public ExternalInterfaceObjectAccessDeniedException(String reason) {
		super(reason);
	}

	public ExternalInterfaceObjectAccessDeniedException(String reason,Throwable cause) {
		super(reason,cause);
	}
}
