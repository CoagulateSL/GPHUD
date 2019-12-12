package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSInvalidPopError extends GSInternalError {
	private static final long serialVersionUID=1L;
	public GSInvalidPopError(String reason) {
		super(reason);
	}

	public GSInvalidPopError(String reason, Throwable cause) {
		super(reason, cause);
	}

	public String toString() { return "{GS Internal Error}: "+getLocalizedMessage(); }
}
