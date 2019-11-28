package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSResourceUnavailableException extends GSException {
	private static final long serialVersionUID=1L;
	public GSResourceUnavailableException(String reason) {
		super(reason);
	}

	public GSResourceUnavailableException(String reason, Throwable cause) {
		super(reason, cause);
	}

	public String toString() { return "{GS Resource Unavailable Exception}: "+getLocalizedMessage(); }
}
