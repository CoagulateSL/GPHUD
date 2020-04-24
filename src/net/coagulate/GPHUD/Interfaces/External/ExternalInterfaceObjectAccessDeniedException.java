package net.coagulate.GPHUD.Interfaces.External;

import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;

public class ExternalInterfaceObjectAccessDeniedException extends UserAccessDeniedException {
	public ExternalInterfaceObjectAccessDeniedException(String reason) {
		super(reason);
	}

	public ExternalInterfaceObjectAccessDeniedException(String reason,
	                                                    Throwable cause) {
		super(reason,cause);
	}
}
