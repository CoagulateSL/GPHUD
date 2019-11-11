package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSResourceLimitExceededException extends GSException {
	public GSResourceLimitExceededException(String reason) {
		super(reason);
	}

	public GSResourceLimitExceededException(String reason, Throwable cause) {
		super(reason, cause);
	}

	public String toString() { return "{GS Resource Limit Exceeded Exception}: "+getLocalizedMessage(); }
}