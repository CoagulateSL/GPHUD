package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSInvalidPopError extends GSInternalError {
	public GSInvalidPopError(String reason) {
		super(reason);
	}

	public GSInvalidPopError(String reason, Throwable cause) {
		super(reason, cause);
	}

	public String toString() { return "{GS Internal Error}: "+getLocalizedMessage(); }
}
