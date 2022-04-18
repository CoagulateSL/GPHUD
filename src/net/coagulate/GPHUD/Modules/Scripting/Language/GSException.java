package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Exceptions.UserException;

public abstract class GSException extends UserException {
	private static final long serialVersionUID = 1L;

	protected GSException(final String reason) {
		super(reason);
	}

	protected GSException(final String reason,
						  final Throwable cause) {
		super(reason, cause);
	}

	protected GSException(final String reason, final boolean suppresslogging) {
		super(reason, suppresslogging);
	}

	protected GSException(final String reason, final Throwable cause, final boolean suppresslogging) {
		super(reason, cause, suppresslogging);
	}
}
