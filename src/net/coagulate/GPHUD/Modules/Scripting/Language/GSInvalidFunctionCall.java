package net.coagulate.GPHUD.Modules.Scripting.Language;

public class GSInvalidFunctionCall extends GSException {
	public GSInvalidFunctionCall(String reason) {
		super(reason);
	}

	public GSInvalidFunctionCall(String reason, Throwable cause) {
		super(reason, cause);
	}

	public String toString() { return "{GS Invalid Function Call Exception}: "+getLocalizedMessage(); }
}
