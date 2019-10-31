package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Tools.UserException;

public class GSException extends UserException {
	public GSException(String reason) {
		super(reason);
	}

	public GSException(String reason, Throwable cause) {
		super(reason, cause);
	}
}
