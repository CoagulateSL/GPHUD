package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSIndexOutOfBoundsException extends GSInvalidExpressionException {
	private static final long serialVersionUID=1L;
	public GSIndexOutOfBoundsException(String reason) {
		super(reason);
	}

	public GSIndexOutOfBoundsException(String reason, Throwable cause) {
		super(reason, cause);
	}
}
