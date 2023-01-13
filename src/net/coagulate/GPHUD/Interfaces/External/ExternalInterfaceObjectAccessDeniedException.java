package net.coagulate.GPHUD.Interfaces.External;

import net.coagulate.Core.Exceptions.User.UserAccessDeniedException;

import java.io.Serial;

public class ExternalInterfaceObjectAccessDeniedException extends UserAccessDeniedException {
	@Serial private static final long serialVersionUID=1L;
	
	public ExternalInterfaceObjectAccessDeniedException(final String message) {
		super(message);
	}
	
	public ExternalInterfaceObjectAccessDeniedException(final String message,final Throwable cause) {
		super(message,cause);
	}
}
