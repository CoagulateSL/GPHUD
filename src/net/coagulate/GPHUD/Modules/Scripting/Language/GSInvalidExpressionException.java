package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSInvalidExpressionException extends GSException {
	private static final long serialVersionUID=1L;
	public GSInvalidExpressionException(String reason) {
		super(reason);
	}

	public GSInvalidExpressionException(String reason, Throwable cause) {
		super(reason, cause);
	}

	public String toString() { return "{GS Invalid Expression Exception}: "+getLocalizedMessage(); }
}
