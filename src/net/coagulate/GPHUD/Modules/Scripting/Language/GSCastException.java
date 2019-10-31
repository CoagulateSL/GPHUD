package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSCastException extends GSInvalidExpressionException {
	public GSCastException(String reason) {
		super(reason);
	}

	public GSCastException(String reason, Throwable cause) {
		super(reason, cause);
	}
}
