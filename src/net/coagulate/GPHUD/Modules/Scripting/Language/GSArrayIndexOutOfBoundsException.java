package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSArrayIndexOutOfBoundsException extends GSInvalidExpressionException {
	public GSArrayIndexOutOfBoundsException(String reason) {
		super(reason);
	}

	public GSArrayIndexOutOfBoundsException(String reason, Throwable cause) {
		super(reason, cause);
	}
}
