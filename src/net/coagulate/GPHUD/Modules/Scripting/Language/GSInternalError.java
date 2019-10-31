package net.coagulate.GPHUD.Modules.Scripting.Language;

import net.coagulate.Core.Tools.SystemException;

public class GSInternalError extends SystemException {
	public GSInternalError(String reason) {
		super(reason);
	}

	public GSInternalError(String reason, Throwable cause) {
		super(reason, cause);
	}

	public static class GSIndexOutOfBoundsException extends GSInvalidExpressionException {
		public GSIndexOutOfBoundsException(String reason) {
			super(reason);
		}

		public GSIndexOutOfBoundsException(String reason, Throwable cause) {
			super(reason, cause);
		}
	}
}
